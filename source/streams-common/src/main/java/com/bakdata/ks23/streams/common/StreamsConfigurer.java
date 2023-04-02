package com.bakdata.ks23.streams.common;

import com.bakdata.ks23.common.BootstrapConfig;
import io.quarkus.arc.Priority;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import java.net.InetSocketAddress;
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
    KafkaStreamsRuntimeConfig streamsConfiguration(final BootstrapConfig bootstrapConfig,
            final BootstrapStreamsConfig streamsConfig) {
        final KafkaStreamsRuntimeConfig runtimeConfig = new KafkaStreamsRuntimeConfig();
        ConfigInstantiator.handleObject(runtimeConfig);
        runtimeConfig.bootstrapServers = List.of(convertBroker(bootstrapConfig.brokers()));
        runtimeConfig.schemaRegistryUrl = Optional.ofNullable(bootstrapConfig.schemaRegistryUrl());
        runtimeConfig.applicationId = streamsConfig.id();
        runtimeConfig.topics = Optional.of(Stream.concat(
                streamsConfig.extraInputTopics().values().stream(),
                streamsConfig.inputTopics().stream().flatMap(List::stream)
        ).toList());
        return runtimeConfig;
    }

    private static InetSocketAddress convertBroker(final String broker) {
        try {
            final String[] split = broker.split(":");
            final int port = Integer.parseInt(split[1]);
            return new InetSocketAddress(split[0], port);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid broker %s".formatted(broker));
        }
    }
}
