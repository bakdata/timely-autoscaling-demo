package com.bakdata.ks23.integrator;

import static com.bakdata.ks23.integrator.PredictionProcessorSupplier.AD_STATE_STORE;
import static com.bakdata.ks23.integrator.PredictionProcessorSupplier.USER_STATE_STORE;

import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.PredictionSample;
import com.bakdata.ks23.integrator.client.AdClient;
import com.bakdata.ks23.integrator.client.Prediction;
import com.bakdata.ks23.integrator.client.UserClient;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;

@Slf4j
public class PredictionProcessor implements FixedKeyProcessor<byte[], FullSample, PredictionSample> {
    public static final Duration TIMEOUT = Duration.ofSeconds(5);
    private FixedKeyProcessorContext<byte[], PredictionSample> context;

    private KeyValueStore<Integer, Double> userStore;

    private KeyValueStore<Integer, Double> adStore;


    private final UserClient userClient;
    private final AdClient adClient;
    private final boolean useCache;
    private final CacheMetric cacheMetric;

    public PredictionProcessor(final UserClient userClient, final AdClient adClient, final boolean useCache,
            final CacheMetric cacheMetric) {
        this.userClient = userClient;
        this.adClient = adClient;
        this.useCache = useCache;
        this.cacheMetric = cacheMetric;
    }


    @Override
    public void init(final FixedKeyProcessorContext<byte[], PredictionSample> context) {
        this.context = context;
        if (this.useCache) {
            log.info("Use cache");
            this.adStore = context.getStateStore(AD_STATE_STORE);
            this.userStore = context.getStateStore(USER_STATE_STORE);
        }
    }

    @Override
    public void process(final FixedKeyRecord<byte[], FullSample> record) {
        final FullSample sample = record.value();

        final Optional<Double> userCachePrediction = this.getFromCache(sample.getUserId(), this.userStore);
        final Optional<Double> adCachePrediction = this.getFromCache(sample.getAdgroupId(), this.adStore);

        if (userCachePrediction.isPresent() && adCachePrediction.isPresent()) {
            this.cacheMetric.increment();
        }

        final PredictionSample predictionSample = Uni.combine().all().unis(
                        callApi(userCachePrediction, sample.getUserId(), this.userClient.newUserPrediction(),
                                this.userStore),
                        callApi(adCachePrediction, sample.getAdgroupId(), this.adClient.newAdPrediction(), this.adStore)
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
                .atMost(TIMEOUT);
        this.context.forward(record.withValue(predictionSample));
    }

    private static Uni<Double> callApi(final Optional<Double> prediction, final int id,
            final Uni<Prediction> predictionCall, final KeyValueStore<Integer, Double> store) {
        return Uni.createFrom().optional(prediction).onItem().ifNull()
                .switchTo(
                        predictionCall.onFailure().retry().atMost(3)
                                .map(Prediction::getScore)
                                .onItem()
                                .invoke(score -> store.put(id, score))
                );
    }

    private Optional<Double> getFromCache(final int id, final KeyValueStore<Integer, Double> store) {
        if (!this.useCache) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }


    @Override
    public void close() {
        FixedKeyProcessor.super.close();
    }
}
