package com.bakdata.ks23;

import io.smallrye.mutiny.Multi;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class SampleProducer {

    private final ZipCsvReader<Sample> zipCsvReader;
    private final ProducerConfig config;

    public SampleProducer(final ProducerConfig config) {
        this.zipCsvReader = new ZipCsvReader<>(Sample::new);
        this.config = config;
    }

    @Outgoing("sample-out")
    public Multi<Sample> produceSamples() {
        if (!this.config.sample().enabled()) {
            return Multi.createFrom().empty();
        }

        return Multi.createBy().combining()
                .streams(
                        Multi.createFrom().ticks().every(Duration.ofMillis(this.config.sample().millisTicks())),
                        Multi.createFrom().items(this.zipCsvReader.readZippedCsv(this.config.zipPath(), "raw_sample.csv"))
                )
                .using((tick, sample) -> sample);
    }

}
