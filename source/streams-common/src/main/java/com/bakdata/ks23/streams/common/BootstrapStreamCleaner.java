package com.bakdata.ks23.streams.common;

import com.bakdata.kafka.CleanUpRunner;
import com.bakdata.kafka.util.ImprovedAdminClient;
import com.bakdata.ks23.common.AdminClientProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

@ApplicationScoped
@Slf4j
public class BootstrapStreamCleaner {


    private final BootstrapStreamsConfig config;
    private final AdminClientProvider clientProvider;

    @Inject
    public BootstrapStreamCleaner(final BootstrapStreamsConfig config, final AdminClientProvider clientProvider) {
        this.config = config;
        this.clientProvider = clientProvider;
    }

    public void runCleanUp(final Topology topology, final KafkaStreams kafkaStreams) {
        try (final ImprovedAdminClient improvedAdminClient = this.clientProvider.newAdminClient()) {
            final CleanUpRunner cleanUpRunner = CleanUpRunner.builder()
                    .topology(topology)
                    .appId(this.config.id())
                    .adminClient(improvedAdminClient)
                    .streams(kafkaStreams)
                    .build();
            cleanUpRunner.run(false);
        }
    }
}
