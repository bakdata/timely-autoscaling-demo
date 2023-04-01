package com.bakdata.ks23.common;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ConfigMapping(prefix = "app")
public interface BootstrapConfig {

    InetSocketAddress brokers();

    String schemaRegistryUrl();

    @WithDefault("false")
    boolean cleanUp();

    Optional<String> outputTopic();

    @WithConverter(BootstrapMapConverter.class)
    Map<String, String> extraOutputTopics();

    @WithConverter(BootstrapMapConverter.class)
    Map<String, String> streamsConfig();


    default List<String> allOutputTopics() {
        return Stream.concat(
                        this.outputTopic().stream(),
                        this.extraOutputTopics().values().stream())
                .toList();
    }

    @WithParentName
    Optional<BootstrapStreamsConfig> streams();

    interface BootstrapStreamsConfig {
        String id();

        @WithConverter(BootstrapListConverter.class)
        Optional<List<String>> inputTopics();

        @WithConverter(BootstrapMapConverter.class)
        Map<String, String> extraInputTopics();

        String errorTopic();
    }

}
