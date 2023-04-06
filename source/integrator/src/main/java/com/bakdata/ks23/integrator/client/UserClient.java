package com.bakdata.ks23.integrator.client;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("predictions")
public interface UserClient {
    @POST
    @Path("users")
    Uni<Prediction> newUserPrediction();

}
