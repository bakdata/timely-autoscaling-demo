package com.bakdata.ks23.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "app")
public interface BootstrapConfig {

    String brokers();

    String schemaRegistryUrl();

    @WithDefault("false")
    boolean cleanUp();

    Optional<String> outputTopic();

    @WithConverter(BootstrapMapConverter.class)
    Map<String, String> extraOutputTopics();

    @WithConverter(BootstrapMapConverter.class)
    Map<String, String> streamsConfig();

}
