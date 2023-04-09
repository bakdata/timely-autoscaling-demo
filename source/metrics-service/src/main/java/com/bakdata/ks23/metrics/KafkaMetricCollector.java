package com.bakdata.ks23.metrics;

import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.Sample;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class KafkaMetricCollector {
    private final UniqueIdCounter usersCounter;
    private final UniqueIdCounter adsCounter;

    public KafkaMetricCollector(@Named(CounterFactory.USERS_COUNTER) final UniqueIdCounter usersCounter,
            @Named(CounterFactory.ADS_COUNTER) final UniqueIdCounter adsCounter) {
        this.usersCounter = usersCounter;
        this.adsCounter = adsCounter;
    }

    @Incoming("samples")
    public void processSample(final Sample sample) {
        this.usersCounter.observeId(sample.getUserId());
        this.adsCounter.observeId(sample.getAdgroupId());
    }

}
