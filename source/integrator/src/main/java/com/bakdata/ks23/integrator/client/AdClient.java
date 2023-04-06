package com.bakdata.ks23.integrator.client;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("predictions")
public interface AdClient {
    @POST
    @Path("ads")
    Uni<Prediction> newAdPrediction();
}
