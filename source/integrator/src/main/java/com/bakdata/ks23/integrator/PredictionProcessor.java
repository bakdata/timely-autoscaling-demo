package com.bakdata.ks23.integrator;

import com.bakdata.ks23.FullSample;
import com.bakdata.ks23.PredictionSample;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;

@Slf4j
public class PredictionProcessor implements FixedKeyProcessor<byte[], FullSample, PredictionSample> {
    public static final Duration TIMEOUT = Duration.ofSeconds(5);
    private FixedKeyProcessorContext<byte[], PredictionSample> context;

    private final UserClient userClient;
    private final AdClient adClient;

    public PredictionProcessor(final UserClient userClient, final AdClient adClient) {
        this.userClient = userClient;
        this.adClient = adClient;
    }

    @Override
    public void init(final FixedKeyProcessorContext<byte[], PredictionSample> context) {
        this.context = context;
    }

    @Override
    public void process(final FixedKeyRecord<byte[], FullSample> record) {
        final FullSample sample = record.value();
        final PredictionSample predictionSample = Uni.combine().all().unis(
                        this.userClient.newUserPrediction().onFailure().retry().atMost(3),
                        this.adClient.newAdPrediction().onFailure().retry().atMost(3)
                )
                .combinedWith((userPrediction, adPrediction) ->
                        PredictionSample.newBuilder()
                                .setUserId(sample.getUserId())
                                .setAdgroupId(sample.getAdgroupId())
                                .setUserScore(userPrediction.getScore())
                                .setAdScore(adPrediction.getScore())
                                .build()
                )
                .await()
                .atMost(TIMEOUT);
        this.context.forward(record.withValue(predictionSample));
    }


    @Override
    public void close() {
        FixedKeyProcessor.super.close();
    }
}
