package com.bakdata.ks23.preprocessing;

import com.bakdata.ks23.AdFeature;
import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.Sample;
import com.bakdata.ks23.UserProfile;
import java.time.Duration;
import java.util.Optional;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

public class JoiningProcessor implements Processor<byte[], Sample, byte[], FullSample> {

    private final Duration ticks;
    private final Duration delayDuration;

    private KeyValueStore<Integer, ValueAndTimestamp<AdFeature>> adFeatureStore;
    private KeyValueStore<Integer, ValueAndTimestamp<UserProfile>> userProfileStore;
    private ProcessorContext<byte[], FullSample> context;

    private KeyValueStore<Long, FullSample> delayStore;

    public JoiningProcessor(final Duration ticks, final Duration delayDuration) {
        this.ticks = ticks;
        this.delayDuration = delayDuration;
    }

    @Override
    public void init(final ProcessorContext<byte[], FullSample> context) {
        this.adFeatureStore = context.getStateStore(PreprocessingTopology.AD_FEATURE_STATE_STORE);
        this.userProfileStore = context.getStateStore(PreprocessingTopology.USER_PROFILE_STATE_STORE);
        this.delayStore = context.getStateStore(JoiningProcessorSupplier.DELAY_STORE_NAME);
        this.context = context;
        this.context.schedule(this.ticks, PunctuationType.WALL_CLOCK_TIME, new DelayPunctuator(
                this.delayDuration.toMillis(),
                this.delayStore,
                context
        ));
    }

    @Override
    public void process(final Record<byte[], Sample> record) {

        final Sample value = record.value();

        final UserProfile userProfile = Optional.ofNullable(this.userProfileStore.get(value.getUserId()))
                .map(ValueAndTimestamp::value)
                .orElse(null);

        final AdFeature adFeature = Optional.ofNullable(this.adFeatureStore.get(value.getAdgroupId()))
                .map(ValueAndTimestamp::value)
                .orElse(null);

        final FullSample build = FullSample.newBuilder()
                .setUserId(value.getUserId())
                .setTimestamp(value.getTimestamp())
                .setAdgroupId(value.getAdgroupId())
                .setPid(value.getPid())
                .setIsClick(value.getIsClick())
                .setUserProfile(userProfile)
                .setAdFeature(adFeature)
                .build();

        this.delayStore.put(System.currentTimeMillis(), build);
    }

    @Override
    public void close() {
        Processor.super.close();
    }

}
