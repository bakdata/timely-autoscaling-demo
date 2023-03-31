package com.bakdata.ks23.producer;

import com.bakdata.ks23.AdFeature;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import javax.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;

@ApplicationScoped
@Slf4j
public class AdFeatureProducer implements KafkaProducer {
    public static final String AD_FEATURE_CHANNEL = "ad-feature";
    private final ZipCsvReader<AdFeature> zipCsvReader;
    private final ProducerConfig producerConfig;
    private final MutinyEmitter<Record<Integer, AdFeature>> emitter;

    public AdFeatureProducer(final ProducerConfig producerConfig,
            @Channel(AD_FEATURE_CHANNEL) final MutinyEmitter<Record<Integer, AdFeature>> emitter) {
        this.producerConfig = producerConfig;
        this.zipCsvReader = new ZipCsvReader<>(AdFeature::new);
        this.emitter = emitter;
    }

    @Override
    public Multi<Void> produceRecords() {
        return Multi.createFrom()
                .items(() -> this.zipCsvReader.readZippedCsv(this.producerConfig.zipPath(), "ad_feature.csv"))
                .map(adFeature -> Record.of(adFeature.getAdGroupId(), adFeature))
                .onItem()
                .transformToUni(this.emitter::send)
                .merge()
                .onSubscription().invoke(() -> log.info("Starting to produce ad-features"))
                .onTermination().invoke(() -> log.info("Finished producing ad-features"));
    }

    @Override
    public boolean enabled() {
        return this.producerConfig.adFeature().enabled();
    }


}
