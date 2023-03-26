package com.bakdata.ks23;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class AdFeatureProducer {
    private final ZipCsvReader<AdFeature> zipCsvReader;
    private final ProducerConfig producerConfig;

    public AdFeatureProducer(final ProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
        this.zipCsvReader = new ZipCsvReader<>(AdFeature::new);
    }

    @Outgoing("ad-feature-out")
    public Multi<Record<Integer, AdFeature>> produceUserProfiles() {
        if (!this.producerConfig.adFeature().enabled()) {
            return Multi.createFrom().empty();
        }

        return Multi.createFrom()
                .items(this.zipCsvReader.readZippedCsv(this.producerConfig.zipPath(), "ad_feature.csv"))
                .map(adFeature -> Record.of(adFeature.getAdGroupId(), adFeature));
    }
}
