package com.bakdata.ks23.common;

import io.smallrye.config.WithConverter;
import java.util.List;

public interface StreamsBootstrapConfig extends BootstrapConfig {

    @WithConverter(BootstrapListConverter.class)
    List<String> inputTopics();

    String inputPattern();

    String errorTopic();

}
