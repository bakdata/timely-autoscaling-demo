package com.bakdata.ks23.rest;


import com.bakdata.ks23.rest.PredictionConfig.Distribution;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Random;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Path("predictions")
@Slf4j
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PredictionResource {
    private final Random random;
    private final PredictionConfig config;

    @Inject
    public PredictionResource(final PredictionConfig config) {
        this.config = config;
        this.random = new Random();
    }

    @POST
    @Path("users")
    public Uni<Prediction> newUserPrediction() {
        return this.newPrediction("user");
    }


    @POST
    @Path("ads")
    public Uni<Prediction> newAdPrediction() {
        return this.newPrediction("ad");
    }

    private Uni<Prediction> newPrediction(final String type) {
        return Uni.createFrom().item(this.predict(type))
                .onItem().call(this::simulateWork);
    }

    private Prediction predict(final String type) {
        return new Prediction(
                this.drawFromDistribution(this.config.prediction()),
                type
        );
    }

    private Uni<?> simulateWork() {
        final long delay = Math.abs((long) this.drawFromDistribution(this.config.timeoutMillis()));
        return Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofMillis(delay));
    }

    private double drawFromDistribution(final Distribution distribution) {
        return this.random.nextGaussian(distribution.mean(), distribution.stdDev());
    }


}
