package com.bakdata.ks23.integrator;

import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.PredictionSample;
import com.bakdata.ks23.integrator.IntegratorConfig.Cache.CacheType;
import com.bakdata.ks23.integrator.client.AdClient;
import com.bakdata.ks23.integrator.client.UserClient;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

@ApplicationScoped
class PredictionProcessorSupplier implements FixedKeyProcessorSupplier<byte[], FullSample, PredictionSample> {

    public static final String USER_STATE_STORE = "cacheStateStoreUser";
    public static final String AD_STATE_STORE = "cacheStateStoreAd";

    private final UserClient userClient;
    private final AdClient adClient;
    private final IntegratorConfig integratorConfig;
    private final CacheMetric cacheMetric;

    PredictionProcessorSupplier(final UserClient userClient, final AdClient adClient,
            final IntegratorConfig integratorConfig, final CacheMetric cacheMetric) {
        this.userClient = userClient;
        this.adClient = adClient;
        this.integratorConfig = integratorConfig;
        this.cacheMetric = cacheMetric;
    }

    @Override
    public FixedKeyProcessor<byte[], FullSample, PredictionSample> get() {
        return new PredictionProcessor(
                this.userClient,
                this.adClient,
                this.integratorConfig.cache().enabled(),
                this.cacheMetric
        );
    }

    @Override
    public Set<StoreBuilder<?>> stores() {
        if (this.integratorConfig.cache().enabled()) {
            return Set.of(this.newPredictionStore(USER_STATE_STORE), this.newPredictionStore(AD_STATE_STORE));
        } else {
            return Set.of();
        }
    }

    private StoreBuilder<KeyValueStore<Integer, Double>> newPredictionStore(final String storeName) {
        final KeyValueBytesStoreSupplier storeSupplier;
        if (this.integratorConfig.cache().type() == CacheType.MEMORY) {
            storeSupplier = Stores.inMemoryKeyValueStore(storeName);
        } else {
            storeSupplier = Stores.persistentKeyValueStore(storeName);
        }
        return Stores.keyValueStoreBuilder(storeSupplier, Serdes.Integer(), Serdes.Double());
    }
}
