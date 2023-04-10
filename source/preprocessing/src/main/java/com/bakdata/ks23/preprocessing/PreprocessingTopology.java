package com.bakdata.ks23.preprocessing;

import com.bakdata.kafka.AvroDeadLetterConverter;
import com.bakdata.kafka.DeadLetter;
import com.bakdata.kafka.ErrorCapturingProcessor;
import com.bakdata.kafka.ProcessedKeyValue;
import com.bakdata.ks23.AdFeature;
import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.Sample;
import com.bakdata.ks23.UserProfile;
import com.bakdata.ks23.common.BootstrapConfig;
import com.bakdata.ks23.streams.common.BootstrapStreamsConfig;
import com.bakdata.ks23.streams.common.SerdeProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
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
    private final SerdeProvider serdeProvider;
    private final JoiningProcessorSupplier processorSupplier;


    @Inject
    public PreprocessingTopology(final BootstrapConfig bootstrapConfig, final BootstrapStreamsConfig streamsConfig,
            final SerdeProvider serdeProvider, final JoiningProcessorSupplier processorSupplier) {
        this.bootstrapConfig = bootstrapConfig;
        this.streamsConfig = streamsConfig;
        this.serdeProvider = serdeProvider;
        this.processorSupplier = processorSupplier;
    }

    @Produces
    public Topology buildTopology() {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();

        final Serde<Integer> keySerde = Serdes.Integer();
        final Serde<UserProfile> profileSerde = this.serdeProvider.avroSerde();
        final Serde<AdFeature> adFeatureSerde = this.serdeProvider.avroSerde();
        final Serde<Sample> sampleSerde = this.serdeProvider.avroSerde();
        final Serde<FullSample> fullSampleSerde = this.serdeProvider.avroSerde();
        final Serde<DeadLetter> deadLetterSerde = this.serdeProvider.avroSerde();

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

        final KStream<byte[], ProcessedKeyValue<byte[], Sample, FullSample>> processedWithError = streamsBuilder.stream(
                        this.streamsConfig.extraInputTopics().get(SAMPLE),
                        Consumed.with(Serdes.ByteArray(), sampleSerde).withName("sample_input_topic")
                )
                .process(
                        ErrorCapturingProcessor.captureErrors(this.processorSupplier),
                        Named.as("sample_join_processor")
                );

        processedWithError.flatMap(
                        ProcessedKeyValue::getErrors,
                        Named.as("join-error-extractor")
                )
                .processValues(
                        AvroDeadLetterConverter.asProcessor("Could not create full sample"),
                        Named.as("join-dead-letter-converter"))
                .to(
                        this.streamsConfig.errorTopic(),
                        Produced.with(Serdes.ByteArray(), deadLetterSerde).withName("error_output_topic")
                );

        processedWithError.flatMapValues(
                        ProcessedKeyValue::getValues,
                        Named.as("join-value-extractor")
                )
                .to(
                        this.bootstrapConfig.outputTopic().orElseThrow(),
                        Produced.with(Serdes.ByteArray(), fullSampleSerde).withName("full_sample_output_topic")
                );

        return streamsBuilder.build();
    }

}
