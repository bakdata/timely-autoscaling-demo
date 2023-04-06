package com.bakdata.ks23.integrator.processor;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Optional;
import org.apache.kafka.streams.state.TimestampedKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

public class CachedPredictionProvider implements PredictionProvider {
    private final PredictionProvider fallback;
    private final TimestampedKeyValueStore<Integer, Double> cacheStore;
    private final Duration cacheRetentionTime;

    public CachedPredictionProvider(final PredictionProvider fallback,
            final TimestampedKeyValueStore<Integer, Double> cacheStore,
            final Duration cacheRetentionTime) {
        this.fallback = fallback;
        this.cacheStore = cacheStore;
        this.cacheRetentionTime = cacheRetentionTime;
    }

    @Override
    public Uni<Double> getPrediction(final int id) {
        final Uni<Double> cachedPrediction = Uni.createFrom().optional(() -> this.getFromCache(id));
        return cachedPrediction.onItem().ifNull().switchTo(() -> this.callFallback(id));
    }

    private Optional<Double> getFromCache(final int id) {
        return Optional.ofNullable(this.cacheStore.get(id))
                .filter(valueAndTimestamp -> {
                    final long timeInCache = System.currentTimeMillis() - valueAndTimestamp.timestamp();
                    return this.cacheRetentionTime.toMillis() - timeInCache > 0;
                })
                .map(ValueAndTimestamp::value);
    }

    private Uni<Double> callFallback(final int id) {
        return this.fallback.getPrediction(id)
                .onItem()
                .invoke(score -> this.cacheStore.put(id, ValueAndTimestamp.make(score, System.currentTimeMillis())));
    }

}
