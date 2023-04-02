package com.bakdata.ks23.streams.common;

import com.bakdata.ks23.common.BootstrapConfig;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@QuarkusMain
@Slf4j
public class StreamsApplication implements QuarkusApplication {
    private final BootstrapConfig bootstrapConfig;
    private final KafkaStreamsProducer streamsProducer;
    private final BootstrapStreamCleaner cleaner;

    @Inject
    public StreamsApplication(final BootstrapConfig bootstrapConfig, final KafkaStreamsProducer streamsProducer,
            final BootstrapStreamCleaner cleaner) {
        this.bootstrapConfig = bootstrapConfig;
        this.streamsProducer = streamsProducer;
        this.cleaner = cleaner;
    }

    @Override
    public int run(final String... args) {
        if (this.bootstrapConfig.cleanUp()) {
            log.info("Running in clean up mode");
            this.cleaner.runCleanUp(this.streamsProducer.getTopology(), this.streamsProducer.getKafkaStreams());
            return 0;
        }

        this.streamsProducer.startKafkaStreams();
        Quarkus.waitForExit();
        return 0;
    }
}
