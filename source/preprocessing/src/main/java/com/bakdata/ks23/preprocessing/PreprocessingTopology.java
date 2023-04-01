package com.bakdata.ks23.preprocessing;

import com.bakdata.ks23.AdFeature;
import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.Sample;
import com.bakdata.ks23.UserProfile;
import com.bakdata.ks23.common.BootstrapConfig;
import com.bakdata.ks23.common.BootstrapConfig.BootstrapStreamsConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Materialized.StoreType;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;

@ApplicationScoped
public class PreprocessingTopology {

    public static final String AD_FEATURE = "ad-feature";
    public static final String USER_PROFILE = "user-profile";
    public static final String SAMPLE = "sample";
    public static final String AD_FEATURE_STATE_STORE = "ad_feature_global";
    public static final String USER_PROFILE_STATE_STORE = "user_profile_global";

    private final BootstrapConfig bootstrapConfig;
    private final BootstrapStreamsConfig streamsConfig;

    @Inject
    public PreprocessingTopology(final BootstrapConfig bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
        this.streamsConfig = bootstrapConfig.streams().orElseThrow();
    }

    @Produces
    public Topology buildTopology() {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();

        final Serde<Integer> keySerde = Serdes.Integer();
        final Serde<UserProfile> profileSerde = this.createSerde();
        final Serde<AdFeature> adFeatureSerde = this.createSerde();
        final Serde<Sample> sampleSerde = this.createSerde();
        final Serde<FullSample> fullSampleSerde = this.createSerde();

        streamsBuilder.globalTable(
                this.streamsConfig.extraInputTopics().get(AD_FEATURE),
                Materialized.<Integer, AdFeature, KeyValueStore<Bytes, byte[]>>as(AD_FEATURE_STATE_STORE)
                        .withKeySerde(keySerde)
                        .withValueSerde(adFeatureSerde)
                        .withStoreType(StoreType.IN_MEMORY) // ~30MB
        );

        streamsBuilder.globalTable(
                this.streamsConfig.extraInputTopics().get(USER_PROFILE),
                Materialized.<Integer, UserProfile, KeyValueStore<Bytes, byte[]>>as(USER_PROFILE_STATE_STORE)
                        .withKeySerde(keySerde)
                        .withValueSerde(profileSerde)
                        .withStoreType(StoreType.IN_MEMORY) // ~30MB
        );

        streamsBuilder.stream(
                        this.streamsConfig.extraInputTopics().get(SAMPLE),
                        Consumed.with(Serdes.ByteArray(), sampleSerde).withName("sample_input_topic")
                )
                .processValues(
                        JoiningProcessor::new,
                        Named.as("sample_join_processor")
                )
                .to(
                        this.bootstrapConfig.outputTopic().orElseThrow(),
                        Produced.with(Serdes.ByteArray(), fullSampleSerde).withName("full_sample_output_topic")
                );

        return streamsBuilder.build();
    }


    private <T extends SpecificRecord> SpecificAvroSerde<T> createSerde() {
        final SpecificAvroSerde<T> avroSerde = new SpecificAvroSerde<>();
        final Map<String, Object> config = new HashMap<>();
        config.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.bootstrapConfig.schemaRegistryUrl());
        avroSerde.configure(config, false);
        return avroSerde;
    }
}
