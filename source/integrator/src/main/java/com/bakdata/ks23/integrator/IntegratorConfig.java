package com.bakdata.ks23.integrator;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "app.integrator")
public interface IntegratorConfig {

    Cache cache();

    String userUrl();

    String adUrl();

    interface Cache {
        boolean enabled();

        @WithDefault("MEMORY")
        CacheType type();

        enum CacheType {
            MEMORY,
            ROCKSDB
        }
    }
}
