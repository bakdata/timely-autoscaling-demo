package com.bakdata.ks23.integrator.processor;

import static com.bakdata.ks23.integrator.processor.PredictionProcessorSupplier.AD_STATE_STORE;
import static com.bakdata.ks23.integrator.processor.PredictionProcessorSupplier.USER_STATE_STORE;

import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.PredictionSample;
import com.bakdata.ks23.integrator.client.AdClient;
import com.bakdata.ks23.integrator.client.Prediction;
import com.bakdata.ks23.integrator.client.UserClient;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;

@Slf4j
public class PredictionProcessor implements FixedKeyProcessor<byte[], FullSample, PredictionSample> {
    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(5);

    private final UserClient userClient;
    private final AdClient adClient;
    private final boolean useCache;
    private final Duration cacheRetentionTime;

    private FixedKeyProcessorContext<byte[], PredictionSample> context;
    private PredictionProvider userPredictionProvider;
    private PredictionProvider adPredictionProvider;

    public PredictionProcessor(final UserClient userClient, final AdClient adClient, final boolean useCache,
            final Duration cacheRetentionTime) {
        this.userClient = userClient;
        this.adClient = adClient;
        this.useCache = useCache;
        this.cacheRetentionTime = cacheRetentionTime;
    }

    @Override
    public void init(final FixedKeyProcessorContext<byte[], PredictionSample> context) {
        this.context = context;
        final PredictionProvider remoteUserProvider =
                id -> this.userClient.newUserPrediction().map(Prediction::getScore);
        final PredictionProvider remoteAdProvider =
                id -> this.adClient.newAdPrediction().map(Prediction::getScore);

        if (this.useCache) {
            log.info("Use cached prediction provider with a retention time of {}", this.cacheRetentionTime);
            this.userPredictionProvider = new CachedPredictionProvider(
                    remoteUserProvider,
                    context.getStateStore(USER_STATE_STORE),
                    this.cacheRetentionTime
            );
            this.adPredictionProvider = new CachedPredictionProvider(
                    remoteAdProvider,
                    context.getStateStore(AD_STATE_STORE),
                    this.cacheRetentionTime
            );
        } else {
            this.userPredictionProvider = remoteUserProvider;
            this.adPredictionProvider = remoteAdProvider;
        }
    }

    @Override
    public void process(final FixedKeyRecord<byte[], FullSample> record) {
        final FullSample sample = record.value();

        final PredictionSample predictionSample = Uni.combine().all().unis(
                        this.userPredictionProvider.getPrediction(sample.getUserId()),
                        this.adPredictionProvider.getPrediction(sample.getAdgroupId())
                )
                .combinedWith((userScore, adScore) ->
                        PredictionSample.newBuilder()
                                .setUserId(sample.getUserId())
                                .setAdgroupId(sample.getAdgroupId())
                                .setUserScore(userScore)
                                .setAdScore(adScore)
                                .build()
                )
                .await()
                .atMost(CLIENT_TIMEOUT);
        this.context.forward(record.withValue(predictionSample));
    }

    @Override
    public void close() {
        FixedKeyProcessor.super.close();
    }

}
