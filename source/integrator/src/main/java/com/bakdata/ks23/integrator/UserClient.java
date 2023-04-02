package com.bakdata.ks23.integrator;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("predictions")
public interface UserClient {
    @POST
    @Path("users")
    Uni<Prediction> newUserPrediction();

}
