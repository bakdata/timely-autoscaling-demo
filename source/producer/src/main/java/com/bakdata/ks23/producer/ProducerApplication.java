package com.bakdata.ks23.producer;

import com.bakdata.ks23.common.BootstrapConfig;
import com.bakdata.ks23.common.BootstrapProducerCleaner;
import io.quarkus.arc.All;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.mutiny.Multi;
import java.util.List;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@QuarkusMain
public class ProducerApplication implements QuarkusApplication {
    private final List<KafkaProducer> producers;
    private final BootstrapProducerCleaner cleaner;
    private final BootstrapConfig bootstrapConfig;

    @Inject
    public ProducerApplication(@All final List<KafkaProducer> producers, final BootstrapProducerCleaner cleaner,
            final Instance<BootstrapConfig> bootstrapConfig) {
        this.producers = producers;
        this.cleaner = cleaner;
        this.bootstrapConfig = bootstrapConfig.get();
    }

    @Override
    public int run(final String... args) {
        if (this.bootstrapConfig.cleanUp()) {
            this.cleaner.runCleanUp();
            return 0;
        }

        final List<Multi<Void>> producingStreams = this.producers.stream()
                .filter(KafkaProducer::enabled)
                .map(KafkaProducer::produceRecords)
                .toList();

        Multi.createBy().merging().streams(producingStreams).toUni().await().indefinitely();
        return 0;
    }

}


