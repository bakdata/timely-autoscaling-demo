package com.bakdata.ks23.producer;

import io.smallrye.mutiny.Multi;

public interface KafkaProducer {
    Multi<Void> produceRecords();

    boolean enabled();

}
