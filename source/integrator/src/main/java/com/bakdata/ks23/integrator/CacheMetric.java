package com.bakdata.ks23.integrator;

import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheMetric {
    private final AtomicInteger integer;

    public CacheMetric() {
        this.integer = new AtomicInteger();
    }

    public void increment() {
        this.integer.incrementAndGet();
    }
}
