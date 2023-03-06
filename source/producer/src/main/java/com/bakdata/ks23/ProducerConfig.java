package com.bakdata.ks23;

import static com.bakdata.ks23.ProducerConfig.PREFIX;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = PREFIX)
public interface ProducerConfig {

    String PREFIX = "ks23.producer";

    Enabled adFeature();

    Enabled userProfile();

    SampleConfig sample();


    interface Enabled {
        @WithDefault("true")
        boolean enabled();
    }

    interface SampleConfig extends Enabled {
        int millisTicks();
    }


}
