package com.bakdata.ks23.preprocessing;

import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.Sample;
import com.bakdata.ks23.streams.common.SerdeProvider;
import java.time.Duration;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

@ApplicationScoped
public class JoiningProcessorSupplier implements ProcessorSupplier<byte[], Sample, byte[], FullSample> {

    public static final String DELAY_STORE_NAME = "delay_store";

    private final PreprocessingConfig config;
    private final SerdeProvider serdeProvider;

    @Inject
    public JoiningProcessorSupplier(final PreprocessingConfig config, final SerdeProvider serdeProvider) {
        this.config = config;
        this.serdeProvider = serdeProvider;
    }

    @Override
    public Processor<byte[], Sample, byte[], FullSample> get() {
        return new JoiningProcessor(Duration.ofMillis(this.config.millisTicks()), this.config.delay());
    }

    @Override
    public Set<StoreBuilder<?>> stores() {
        final Serde<FullSample> valueSerde = this.serdeProvider.avroSerde();
        return Set.of(
                Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore(DELAY_STORE_NAME), Serdes.Long(), valueSerde)
        );
    }

}
