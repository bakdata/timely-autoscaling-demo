package com.bakdata.ks23.producer;

import com.bakdata.ks23.Sample;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;

@ApplicationScoped
@Slf4j
public class SampleProducer implements KafkaProducer {

    public static final String SAMPLE_CHANNEL = "sample";
    private final ZipCsvReader<Sample> zipCsvReader;
    private final ProducerConfig config;
    private final MutinyEmitter<Sample> emitter;

    public SampleProducer(final ProducerConfig config, @Channel(SAMPLE_CHANNEL) final MutinyEmitter<Sample> emitter) {
        this.zipCsvReader = new ZipCsvReader<>(Sample::new);
        this.config = config;
        this.emitter = emitter;
    }

    @Override
    public Multi<Void> produceRecords() {
        return Multi.createBy().combining()
                .streams(
                        Multi.createFrom().ticks().every(Duration.ofMillis(this.config.sample().millisTicks())),
                        Multi.createFrom()
                                .items(() -> this.zipCsvReader.readZippedCsv(this.config.zipPath(), "raw_sample.csv"))

                )
                .using((tick, sample) -> sample)
                .onItem()
                .transformToUni(this.emitter::send)
                .merge()
                .onSubscription().invoke(() -> log.info("Starting to produce user-profiles"))
                .onTermination().invoke(() -> log.info("Finished producing user-profiles"));
    }

    @Override
    public boolean enabled() {
        return this.config.sample().enabled();
    }

}
