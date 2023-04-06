package com.bakdata.ks23.integrator.processor;

import io.smallrye.mutiny.Uni;

public interface PredictionProvider {
    Uni<Double> getPrediction(final int id);
}
