package com.bakdata.ks23.metrics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

@ApplicationScoped
public class CounterFactory {
    public static final String USERS_COUNTER = "users";
    public static final String ADS_COUNTER = "ads";
    private final MetricServiceConfig serviceConfig;
    private final ScheduledExecutorService service;

    public CounterFactory(final MetricServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.service = Executors.newScheduledThreadPool(1);
    }

    @Produces
    @ApplicationScoped
    @Named(USERS_COUNTER)
    public UniqueIdCounter usersCounter() {
        return new UniqueIdCounter(
                this.serviceConfig.userCacheRetention().getSeconds(),
                this.serviceConfig.windowSize().getSeconds(),
                this.service
        );
    }


    @Produces
    @ApplicationScoped
    @Named(ADS_COUNTER)
    public UniqueIdCounter adsCounter() {
        return new UniqueIdCounter(
                this.serviceConfig.adsCacheRetention().getSeconds(),
                this.serviceConfig.windowSize().getSeconds(),
                this.service
        );
    }
}
