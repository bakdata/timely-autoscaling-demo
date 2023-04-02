package com.bakdata.ks23.integrator;

import static com.bakdata.kafka.ErrorCapturingValueProcessor.captureErrors;

import com.bakdata.kafka.AvroDeadLetterConverter;
import com.bakdata.kafka.DeadLetter;
import com.bakdata.kafka.ProcessedValue;
import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.PredictionSample;
import com.bakdata.ks23.common.BootstrapConfig;
import com.bakdata.ks23.streams.common.BootstrapStreamsConfig;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.ProcessingException;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

@ApplicationScoped
public class IntegratorTopology {

    private final BootstrapConfig bootstrapConfig;
    private final BootstrapStreamsConfig streamsConfig;
    private final UserClient userClient;
    private final AdClient adClient;

    @Inject
    public IntegratorTopology(final BootstrapConfig bootstrapConfig, final BootstrapStreamsConfig streamsConfig,
            final IntegratorConfig integratorConfig) {
        this.bootstrapConfig = bootstrapConfig;
        this.streamsConfig = streamsConfig;
        final HostnameVerifier hostnameVerifier = new NoOpVerifier();
        this.userClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(integratorConfig.userUrl()))
                .hostnameVerifier(hostnameVerifier)
                .build(UserClient.class);
        this.adClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(integratorConfig.adUrl()))
                .hostnameVerifier(hostnameVerifier)
                .build(AdClient.class);
    }

    @Produces
    Topology integratorTopology() {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final Serde<FullSample> fullSampleSerde = this.createSerde();
        final Serde<PredictionSample> predictionSampleSerde = this.createSerde();
        final Serde<DeadLetter> deadLetterSerde = this.createSerde();

        final KStream<byte[], ProcessedValue<FullSample, PredictionSample>> processedWithError = streamsBuilder.stream(
                        this.streamsConfig.inputTopics().orElseThrow().get(0),
                        Consumed.with(Serdes.ByteArray(), fullSampleSerde).withName("full_sample_input_topic")
                )
                .processValues(
                        captureErrors(
                                () -> new PredictionProcessor(this.userClient, this.adClient),
                                // Stop processing if a rest api isn't reachable
                                exception -> exception instanceof ProcessingException
                        ),
                        Named.as("predictor_processor")
                );

        processedWithError.flatMapValues(
                        ProcessedValue::getErrors,
                        Named.as("predictor-error-extractor")
                )
                .processValues(
                        AvroDeadLetterConverter.asProcessor("Could not create full sample"),
                        Named.as("predictor-dead-letter-converter"))
                .to(
                        this.streamsConfig.errorTopic(),
                        Produced.with(Serdes.ByteArray(), deadLetterSerde).withName("error_output_topic")
                );

        processedWithError.flatMapValues(
                        ProcessedValue::getValues,
                        Named.as("prediction-value-extractor")
                )
                .to(
                        this.bootstrapConfig.outputTopic().orElseThrow(),
                        Produced.with(Serdes.ByteArray(), predictionSampleSerde).withName("full_sample_output_topic")
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

    private static class NoOpVerifier implements HostnameVerifier {
        @Override
        public boolean verify(final String host, final SSLSession sslSession) {
            return true;
        }
    }
}
