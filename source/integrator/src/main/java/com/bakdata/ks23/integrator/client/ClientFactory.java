package com.bakdata.ks23.integrator.client;

import com.bakdata.ks23.integrator.IntegratorConfig;
import java.net.URI;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.net.ssl.HostnameVerifier;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

public class ClientFactory {
    private final HostnameVerifier hostnameVerifier;
    private final IntegratorConfig integratorConfig;

    public ClientFactory(final HostnameVerifier hostnameVerifier, final IntegratorConfig integratorConfig) {
        this.hostnameVerifier = hostnameVerifier;
        this.integratorConfig = integratorConfig;
    }

    @Produces
    @ApplicationScoped
    UserClient userClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(this.integratorConfig.userUrl()))
                .hostnameVerifier(this.hostnameVerifier)
                .build(UserClient.class);
    }

    @Produces
    @ApplicationScoped
    AdClient adClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(this.integratorConfig.adUrl()))
                .hostnameVerifier(this.hostnameVerifier)
                .build(AdClient.class);
    }

}
