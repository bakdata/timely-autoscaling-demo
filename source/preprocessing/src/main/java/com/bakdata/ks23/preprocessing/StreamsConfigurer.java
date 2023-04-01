package com.bakdata.ks23.preprocessing;

import com.bakdata.ks23.common.BootstrapConfig;
import com.bakdata.ks23.common.BootstrapConfig.BootstrapStreamsConfig;
import io.quarkus.arc.Priority;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

@Dependent
public class StreamsConfigurer {

    @Produces
    @Alternative
    @Priority(1)
    KafkaStreamsRuntimeConfig streamsConfiguration(final BootstrapConfig bootstrapConfig) {
        final KafkaStreamsRuntimeConfig runtimeConfig = new KafkaStreamsRuntimeConfig();
        ConfigInstantiator.handleObject(runtimeConfig);
        runtimeConfig.bootstrapServers = List.of(bootstrapConfig.brokers());
        runtimeConfig.schemaRegistryUrl = Optional.ofNullable(bootstrapConfig.schemaRegistryUrl());
        final Optional<BootstrapStreamsConfig> streams = bootstrapConfig.streams();
        if (streams.isEmpty()) {
            throw new IllegalArgumentException("Streams config must be present");
        }
        final BootstrapStreamsConfig bootstrapStreamsConfig = streams.get();
        runtimeConfig.applicationId = bootstrapStreamsConfig.id();
        runtimeConfig.topics = Optional.of(Stream.concat(
                bootstrapStreamsConfig.extraInputTopics().values().stream(),
                bootstrapStreamsConfig.inputTopics().stream().flatMap(List::stream)
        ).toList());
        return runtimeConfig;
    }
}
