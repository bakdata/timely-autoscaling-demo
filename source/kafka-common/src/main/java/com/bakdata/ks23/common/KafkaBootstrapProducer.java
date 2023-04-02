package com.bakdata.ks23.common;


import io.smallrye.common.annotation.Identifier;
import io.smallrye.config.SmallRyeConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;

public class KafkaBootstrapProducer {

    private static final Pattern PATTERN = Pattern.compile("[^a-z0-9.]");
    private final SmallRyeConfig config;
    private final BootstrapConfig bootstrapConfig;

    @Inject
    public KafkaBootstrapProducer(final SmallRyeConfig config, final BootstrapConfig bootstrapConfig) {
        this.config = config;
        this.bootstrapConfig = bootstrapConfig;
    }

    @Produces
    @ApplicationScoped
    @Identifier("default-kafka-broker")
    public Map<String, Object> createKafkaRuntimeConfig() {
        final Map<String, Object> properties = new HashMap<>();

        StreamSupport.stream(this.config.getPropertyNames().spliterator(), false)
                .map(String::toLowerCase)
                .filter(name -> name.startsWith("kafka"))
                .distinct()
                .sorted()
                .forEach(name -> {
                    final String key =
                            PATTERN.matcher(name.substring("kafka".length() + 1).toLowerCase()).replaceAll(".");
                    final String value = this.config.getOptionalValue(name, String.class).orElse("");
                    properties.put(key, value);
                });

        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapConfig.brokers());
        properties.put("schema.registry.url", this.bootstrapConfig.schemaRegistryUrl());
        properties.putAll(this.bootstrapConfig.streamsConfig());
        return properties;
    }

}
