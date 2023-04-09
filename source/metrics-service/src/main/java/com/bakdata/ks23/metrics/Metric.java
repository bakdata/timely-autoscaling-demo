package com.bakdata.ks23.metrics;

import lombok.Value;

@Value
public class Metric {

    UniqueElements uniqueElements;

    @Value
    static class UniqueElements {
        double users;
        double ads;
    }
}
