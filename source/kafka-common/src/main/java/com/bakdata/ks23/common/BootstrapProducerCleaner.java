package com.bakdata.ks23.common;

import com.bakdata.kafka.CleanUpException;
import com.bakdata.kafka.util.ImprovedAdminClient;
import com.bakdata.kafka.util.SchemaTopicClient;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BootstrapProducerCleaner {
    public static final int RESET_SLEEP_MS = 5000;

    private final BootstrapConfig config;
    private final AdminClientProvider clientProvider;

    @Inject
    public BootstrapProducerCleaner(final BootstrapConfig config, final AdminClientProvider clientProvider) {
        this.config = config;
        this.clientProvider = clientProvider;
    }

    public void runCleanUp() {
        try (final ImprovedAdminClient improvedAdminClient = this.clientProvider.newAdminClient()) {
            final SchemaTopicClient schemaTopicClient = improvedAdminClient.getSchemaTopicClient();

            final Iterable<String> outputTopics = Stream.concat(
                            this.config.outputTopic().stream(),
                            this.config.extraOutputTopics().values().stream())
                    .toList();

            outputTopics.forEach(schemaTopicClient::deleteTopicAndResetSchemaRegistry);
            try {
                Thread.sleep(RESET_SLEEP_MS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CleanUpException("Error waiting for clean up", e);
            }
        }
    }
}
