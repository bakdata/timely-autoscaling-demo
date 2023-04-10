package com.bakdata.ks23.streams.common;

import com.bakdata.ks23.common.BootstrapConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.apache.avro.specific.SpecificRecord;

@ApplicationScoped
public class SerdeProvider {
    private final BootstrapConfig bootstrapConfig;

    public SerdeProvider(final BootstrapConfig bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
    }

    public <T extends SpecificRecord> SpecificAvroSerde<T> avroSerde() {
        final SpecificAvroSerde<T> avroSerde = new SpecificAvroSerde<>();
        final Map<String, Object> config = new HashMap<>();
        config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.bootstrapConfig.schemaRegistryUrl());
        avroSerde.configure(config, false);
        return avroSerde;
    }
}
