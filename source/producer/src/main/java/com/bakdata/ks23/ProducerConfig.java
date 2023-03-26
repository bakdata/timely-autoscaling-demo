package com.bakdata.ks23;

import static com.bakdata.ks23.ProducerConfig.PREFIX;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.nio.file.Path;

@ConfigMapping(prefix = PREFIX)
public interface ProducerConfig {

    String PREFIX = "app.ks23.producer";
    String DEFAULT_ZIP_FILE = "/data.zip";

    Enabled adFeature();

    Enabled userProfile();

    SampleConfig sample();

    @WithDefault(DEFAULT_ZIP_FILE)
    Path zipPath();


    interface Enabled {
        @WithDefault("false")
        boolean enabled();
    }

    interface SampleConfig extends Enabled {
        @WithDefault("500")
        int millisTicks();
    }


}
