package com.bakdata.ks23.metrics;

import java.time.Instant;
import lombok.Value;

@Value
public class Stats {
    CounterStats users;
    CounterStats ads;

    @Value
    static class CounterStats {
        int numberId;
        int numberTimestamps;
        Instant firstTimestamp;
        Instant lastTimestamp;
    }
}

