package com.bakdata.ks23;

import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.AvroSerde;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;

@ApplicationScoped
public class PreprocessingTopology {

    private final KafkaStreamsRuntimeConfig runtimeConfig;

    public PreprocessingTopology(final KafkaStreamsRuntimeConfig config) {
        this.runtimeConfig = config;
    }


    @Produces

    public Topology buildTopology() {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();

        final Serde<Integer> keySerde = Serdes.Integer();
        final Serde<UserProfile> profileSerde = this.createSerde();
        final Serde<AdFeature> adFeatureSerde = this.createSerde();
        final Serde<Sample> sampleSerde = this.createSerde();

        final GlobalKTable<Integer, AdFeature> featureTable =
                streamsBuilder.globalTable("ad-feature", Materialized.with(keySerde, adFeatureSerde));
        final GlobalKTable<Integer, UserProfile> profileTable =
                streamsBuilder.globalTable("user-profile", Materialized.with(keySerde, profileSerde));

        streamsBuilder.stream("sample", Consumed.with(Serdes.ByteArray(), sampleSerde))
                .process(new ProcessorSupplier<byte[], Sample, String, FullSample>() {

                    @Override
                    public Processor<byte[], Sample, String, FullSample> get() {
                        return null;
                    }
                })
    }

    private <T extends SpecificRecord> AvroSerde<T> createSerde() {
        final AvroSerde<T> avroSerde = new AvroSerde<>();
        final Map<String, Object> config = new HashMap<>();
        config.put(SerdeConfig.REGISTRY_URL, this.runtimeConfig.schemaRegistryUrl.orElseThrow());
        config.put(AvroKafkaSerdeConfig.USE_SPECIFIC_AVRO_READER, true);
        avroSerde.configure(config, false);
        return avroSerde;
    }
}
