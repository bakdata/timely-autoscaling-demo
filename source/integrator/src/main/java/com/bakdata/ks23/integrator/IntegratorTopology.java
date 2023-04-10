package com.bakdata.ks23.integrator;

import com.bakdata.kafka.AvroDeadLetterConverter;
import com.bakdata.kafka.DeadLetter;
import com.bakdata.kafka.ErrorCapturingValueProcessor;
import com.bakdata.kafka.ProcessedValue;
import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.PredictionSample;
import com.bakdata.ks23.common.BootstrapConfig;
import com.bakdata.ks23.integrator.processor.PredictionProcessorSupplier;
import com.bakdata.ks23.streams.common.BootstrapStreamsConfig;
import com.bakdata.ks23.streams.common.SerdeProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;

@ApplicationScoped
public class IntegratorTopology {

    private final BootstrapConfig bootstrapConfig;
    private final BootstrapStreamsConfig streamsConfig;
    private final PredictionProcessorSupplier processorSupplier;
    private final SerdeProvider serdeProvider;

    @Inject
    public IntegratorTopology(final BootstrapConfig bootstrapConfig, final BootstrapStreamsConfig streamsConfig,
            final PredictionProcessorSupplier processorSupplier, final SerdeProvider serdeProvider) {
        this.bootstrapConfig = bootstrapConfig;
        this.streamsConfig = streamsConfig;
        this.processorSupplier = processorSupplier;
        this.serdeProvider = serdeProvider;
    }

    @Produces
    Topology integratorTopology() {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final Serde<FullSample> fullSampleSerde = this.serdeProvider.avroSerde();
        final Serde<PredictionSample> predictionSampleSerde = this.serdeProvider.avroSerde();
        final Serde<DeadLetter> deadLetterSerde = this.serdeProvider.avroSerde();

        final KStream<byte[], ProcessedValue<FullSample, PredictionSample>> processedWithError = streamsBuilder.stream(
                        this.streamsConfig.inputTopics()
                                .orElseThrow(() -> new IllegalArgumentException("Input topics must be set")),
                        Consumed.with(Serdes.ByteArray(), fullSampleSerde).withName("full_sample_input_topic")
                )
                .processValues(
                        ErrorCapturingValueProcessor.captureErrors(
                                this.processorSupplier,
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
                        Named.as("predictor-dead-letter-converter")
                )
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

}
