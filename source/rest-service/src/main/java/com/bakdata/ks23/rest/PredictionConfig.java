package com.bakdata.ks23.rest;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "ks23.rest")
public interface PredictionConfig {

    Distribution prediction();

    Distribution latencyMillis();

    Distribution blockingMillis();


    interface Distribution {
        double mean();

        double stdDev();
    }

}
