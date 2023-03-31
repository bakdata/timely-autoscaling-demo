package com.bakdata.ks23.common;

import com.bakdata.kafka.util.ImprovedAdminClient;
import java.time.Duration;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;

@ApplicationScoped
public class AdminClientProvider {

    public static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(10L);

    private final BootstrapConfig bootstrapConfig;

    @Inject
    public AdminClientProvider(final BootstrapConfig bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
    }

    public ImprovedAdminClient newAdminClient() {
        final Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapConfig.brokers());
        properties.put(AdminClientConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, "10000");
        return ImprovedAdminClient.builder()
                .properties(properties)
                .schemaRegistryUrl(this.bootstrapConfig.schemaRegistryUrl())
                .timeout(ADMIN_TIMEOUT)
                .build();
    }
}
