package com.bakdata.ks23.metrics;

import io.smallrye.config.ConfigMapping;
import java.time.Duration;

@ConfigMapping(prefix = "ks23.metrics")
public interface MetricServiceConfig {

    Duration userCacheRetention();

    Duration adsCacheRetention();

    Duration windowSize();

    String sampleTopic();
}
