package com.bakdata.ks23.preprocessing;

import com.bakdata.ks23.AdFeature;
import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.Sample;
import com.bakdata.ks23.UserProfile;
import java.util.Optional;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

public class JoiningProcessor implements FixedKeyProcessor<byte[], Sample, FullSample> {


    private KeyValueStore<Integer, ValueAndTimestamp<AdFeature>> adFeatureStore;
    private KeyValueStore<Integer, ValueAndTimestamp<UserProfile>> userProfileStore;
    private FixedKeyProcessorContext<byte[], FullSample> context;

    @Override
    public void init(final FixedKeyProcessorContext<byte[], FullSample> context) {
        this.adFeatureStore = context.getStateStore(PreprocessingTopology.AD_FEATURE_STATE_STORE);
        this.userProfileStore = context.getStateStore(PreprocessingTopology.USER_PROFILE_STATE_STORE);
        this.context = context;
    }

    @Override
    public void process(final FixedKeyRecord<byte[], Sample> record) {

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

        this.context.forward(record.withValue(build));
    }




    @Override
    public void close() {
        FixedKeyProcessor.super.close();
    }
}
