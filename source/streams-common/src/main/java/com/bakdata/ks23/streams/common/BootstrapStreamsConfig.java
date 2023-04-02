package com.bakdata.ks23.streams.common;

import com.bakdata.ks23.common.BootstrapListConverter;
import com.bakdata.ks23.common.BootstrapMapConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "app")
public interface BootstrapStreamsConfig {

    String id();

    @WithConverter(BootstrapListConverter.class)
    Optional<List<String>> inputTopics();

    @WithConverter(BootstrapMapConverter.class)
    Map<String, String> extraInputTopics();

    String errorTopic();
}
