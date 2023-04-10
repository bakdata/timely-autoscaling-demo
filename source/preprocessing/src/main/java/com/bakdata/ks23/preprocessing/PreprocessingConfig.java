package com.bakdata.ks23.preprocessing;

import io.smallrye.config.ConfigMapping;
import java.time.Duration;

@ConfigMapping(prefix = "app.ks23.preprocessing")
public interface PreprocessingConfig {

    int millisTicks();

    Duration delay();

}
