package com.bakdata.ks23.integrator.client;

import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

@ApplicationScoped
class NoOpVerifier implements HostnameVerifier {
    @Override
    public boolean verify(final String host, final SSLSession sslSession) {
        return true;
    }
}
