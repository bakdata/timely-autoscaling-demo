package com.bakdata.ks23;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerBuilder;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class ApplicationTest {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    void testProcessor() {
        ConsumerBuilder<byte[], byte[]> builder = companion.consumeWithDeserializers(
                ByteArrayDeserializer.class, ByteArrayDeserializer.class
        );
        ConsumerTask<byte[], byte[]> profiles = builder.fromTopics("user-profile", 499);
        profiles.awaitCompletion();
        assertEquals(499, profiles.count());
    }
}
