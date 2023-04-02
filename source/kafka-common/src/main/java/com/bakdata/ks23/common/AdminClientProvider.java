package com.bakdata.ks23.common;

import com.bakdata.kafka.util.ImprovedAdminClient;
import java.time.Duration;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;

@ApplicationScoped
@Slf4j
public class AdminClientProvider {

    public static final Duration ADMIN_TIMEOUT = Duration.ofSeconds(10L);

    private final BootstrapConfig bootstrapConfig;

    @Inject
    public AdminClientProvider(final BootstrapConfig bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
    }

    public ImprovedAdminClient newAdminClient() {
        final Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapConfig.brokers());
        properties.put(AdminClientConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, "10000");
        log.info("Create admin client with properties: {}", properties);

        return ImprovedAdminClient.builder()
                .properties(properties)
                .schemaRegistryUrl(this.bootstrapConfig.schemaRegistryUrl())
                .timeout(ADMIN_TIMEOUT)
                .build();
    }
}
