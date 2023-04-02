package com.bakdata.ks23.streams.common;


import static io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig.DEFAULT_KAFKA_BROKER;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.kafka.streams.runtime.KafkaStreamsPropertiesUtil;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsSupport;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;
import io.quarkus.kafka.streams.runtime.KeyConfig;
import io.quarkus.kafka.streams.runtime.KeyStoreConfig;
import io.quarkus.kafka.streams.runtime.SaslConfig;
import io.quarkus.kafka.streams.runtime.SslConfig;
import io.quarkus.kafka.streams.runtime.TrustStoreConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.smallrye.common.annotation.Identifier;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KafkaStreams.StateListener;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.processor.StateRestoreListener;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Manages the lifecycle of a Kafka Streams pipeline. If there's a producer method returning a KS {@link Topology}, then
 * this topology will be configured and started. Optionally, before starting the pipeline, this manager will wait for a
 * given set of topics to be created, as KS itself will fail without all input topics being created upfront.
 *
 * <p>
 * Taken from io.quarkus.kafka.streams.runtime.KafkaStreamsProducer.
 * <p>
 * Modifications for supporting clean up:
 * <ul>
 * <li> Extracting start into separate method
 * <li> Getter for topology
 */
@Singleton
public class KafkaStreamsProducer {

    private static final Logger LOGGER = Logger.getLogger(KafkaStreamsProducer.class.getName());
    private static volatile boolean shutdown = false;

    private final ExecutorService executorService;
    private final KafkaStreams kafkaStreams;
    private final KafkaStreamsTopologyManager kafkaStreamsTopologyManager;
    private final Admin kafkaAdminClient;
    private final KafkaStreamsRuntimeConfig runtimeConfig;
    private final Topology topology;

    @Inject
    public KafkaStreamsProducer(final KafkaStreamsSupport kafkaStreamsSupport,
            final KafkaStreamsRuntimeConfig runtimeConfig,
            final Instance<Topology> topology, final Instance<KafkaClientSupplier> kafkaClientSupplier,
            @Identifier("default-kafka-broker") final Instance<Map<String, Object>> defaultConfiguration,
            final Instance<StateListener> stateListener,
            final Instance<StateRestoreListener> globalStateRestoreListener,
            final Instance<StreamsUncaughtExceptionHandler> uncaughtExceptionHandlerListener) {
        shutdown = false;
        // No producer for Topology -> nothing to do
        if (topology.isUnsatisfied()) {
            LOGGER.warn("No Topology producer; Kafka Streams will not be started");
            this.executorService = null;
            this.kafkaStreams = null;
            this.kafkaStreamsTopologyManager = null;
            this.kafkaAdminClient = null;
            this.runtimeConfig = null;
            this.topology = null;
            return;
        }

        final Properties buildTimeProperties = kafkaStreamsSupport.getProperties();

        String bootstrapServersConfig = asString(runtimeConfig.bootstrapServers);
        if (DEFAULT_KAFKA_BROKER.equalsIgnoreCase(bootstrapServersConfig)) {
            // Try to see if kafka.bootstrap.servers is set, if so, use that value, if not, keep localhost:9092
            bootstrapServersConfig =
                    ConfigProvider.getConfig().getOptionalValue("kafka.bootstrap.servers", String.class)
                            .orElse(bootstrapServersConfig);
        }
        Map<String, Object> cfg = Collections.emptyMap();
        if (!defaultConfiguration.isUnsatisfied()) {
            cfg = defaultConfiguration.get();
        }
        this.runtimeConfig = runtimeConfig;
        this.topology = topology.get();

        final Properties kafkaStreamsProperties = getStreamsProperties(buildTimeProperties, cfg, bootstrapServersConfig,
                runtimeConfig);
        this.kafkaAdminClient = Admin.create(getAdminClientConfig(kafkaStreamsProperties));

        this.executorService = Executors.newSingleThreadExecutor();

        this.kafkaStreams = initializeKafkaStreams(
                kafkaStreamsProperties,
                this.topology,
                kafkaClientSupplier, stateListener, globalStateRestoreListener,
                uncaughtExceptionHandlerListener
        );
        this.kafkaStreamsTopologyManager = new KafkaStreamsTopologyManager(this.kafkaAdminClient);
    }

    @PostConstruct
    public void postConstruct() {
        if (this.kafkaStreams != null) {
            Arc.container().beanManager().getEvent().select(KafkaStreams.class).fire(this.kafkaStreams);
        }
    }

    @Produces
    @Singleton
    @Unremovable
    @Startup
    public KafkaStreams getKafkaStreams() {
        return this.kafkaStreams;
    }

    @Produces
    @Singleton
    @Unremovable
    @Startup
    public KafkaStreamsTopologyManager kafkaStreamsTopologyManager() {
        return this.kafkaStreamsTopologyManager;
    }

    public Topology getTopology() {
        return this.topology;
    }

    void onStop(@Observes final ShutdownEvent event) {
        shutdown = true;
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
        if (this.kafkaStreams != null) {
            LOGGER.debug("Stopping Kafka Streams pipeline");
            this.kafkaStreams.close();
        }
        if (this.kafkaAdminClient != null) {
            this.kafkaAdminClient.close(Duration.ZERO);
        }
    }

    private static KafkaStreams initializeKafkaStreams(final Properties kafkaStreamsProperties,
            final Topology topology,
            final Instance<KafkaClientSupplier> kafkaClientSupplier,
            final Instance<StateListener> stateListener,
            final Instance<StateRestoreListener> globalStateRestoreListener,
            final Instance<StreamsUncaughtExceptionHandler> uncaughtExceptionHandlerListener) {
        final KafkaStreams kafkaStreams;
        if (kafkaClientSupplier.isUnsatisfied()) {
            kafkaStreams = new KafkaStreams(topology, kafkaStreamsProperties);
        } else {
            kafkaStreams = new KafkaStreams(topology, kafkaStreamsProperties, kafkaClientSupplier.get());
        }

        if (!stateListener.isUnsatisfied()) {
            kafkaStreams.setStateListener(stateListener.get());
        }
        if (!globalStateRestoreListener.isUnsatisfied()) {
            kafkaStreams.setGlobalStateRestoreListener(globalStateRestoreListener.get());
        }
        if (!uncaughtExceptionHandlerListener.isUnsatisfied()) {
            kafkaStreams.setUncaughtExceptionHandler(uncaughtExceptionHandlerListener.get());
        }

        return kafkaStreams;
    }

    public void startKafkaStreams() {
        this.executorService.execute(() -> {
            if (this.runtimeConfig.topicsTimeout.compareTo(Duration.ZERO) > 0) {
                try {
                    waitForTopicsToBeCreated(this.kafkaAdminClient, this.runtimeConfig.getTrimmedTopics(),
                            this.runtimeConfig.topicsTimeout);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (!shutdown) {
                LOGGER.debug("Starting Kafka Streams pipeline");
                this.kafkaStreams.start();
            }
        });
    }

    /**
     * Returns all properties to be passed to Kafka Streams.
     */
    private static Properties getStreamsProperties(final Properties properties,
            final Map<String, Object> cfg,
            final String bootstrapServersConfig,
            final KafkaStreamsRuntimeConfig runtimeConfig) {
        final Properties streamsProperties = new Properties();

        // build-time options
        streamsProperties.putAll(properties);

        // default configuration
        streamsProperties.putAll(cfg);

        // dynamic add -- back-compatibility
        streamsProperties.putAll(KafkaStreamsPropertiesUtil.quarkusKafkaStreamsProperties());
        streamsProperties.putAll(KafkaStreamsPropertiesUtil.appKafkaStreamsProperties());

        // add runtime options
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig);
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, runtimeConfig.applicationId);

        // app id
        if (runtimeConfig.applicationServer.isPresent()) {
            streamsProperties.put(StreamsConfig.APPLICATION_SERVER_CONFIG, runtimeConfig.applicationServer.get());
        }

        // schema registry
        if (runtimeConfig.schemaRegistryUrl.isPresent()) {
            streamsProperties.put(runtimeConfig.schemaRegistryKey, runtimeConfig.schemaRegistryUrl.get());
        }

        // set the security protocol (in case we are doing PLAIN_TEXT)
        setProperty(runtimeConfig.securityProtocol, streamsProperties, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG);

        // sasl
        final SaslConfig sc = runtimeConfig.sasl;
        if (sc != null) {
            setProperty(sc.mechanism, streamsProperties, SaslConfigs.SASL_MECHANISM);

            setProperty(sc.jaasConfig, streamsProperties, SaslConfigs.SASL_JAAS_CONFIG);

            setProperty(sc.clientCallbackHandlerClass, streamsProperties,
                    SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS);

            setProperty(sc.loginCallbackHandlerClass, streamsProperties, SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS);
            setProperty(sc.loginClass, streamsProperties, SaslConfigs.SASL_LOGIN_CLASS);

            setProperty(sc.kerberosServiceName, streamsProperties, SaslConfigs.SASL_KERBEROS_SERVICE_NAME);
            setProperty(sc.kerberosKinitCmd, streamsProperties, SaslConfigs.SASL_KERBEROS_KINIT_CMD);
            setProperty(sc.kerberosTicketRenewWindowFactor, streamsProperties,
                    SaslConfigs.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR);
            setProperty(sc.kerberosTicketRenewJitter, streamsProperties, SaslConfigs.SASL_KERBEROS_TICKET_RENEW_JITTER);
            setProperty(sc.kerberosMinTimeBeforeRelogin, streamsProperties,
                    SaslConfigs.SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN);

            setProperty(sc.loginRefreshWindowFactor, streamsProperties, SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_FACTOR);
            setProperty(sc.loginRefreshWindowJitter, streamsProperties, SaslConfigs.SASL_LOGIN_REFRESH_WINDOW_JITTER);

            setProperty(sc.loginRefreshMinPeriod, streamsProperties, SaslConfigs.SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS,
                    DurationToSecondsFunction.INSTANCE);
            setProperty(sc.loginRefreshBuffer, streamsProperties, SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS,
                    DurationToSecondsFunction.INSTANCE);
        }

        // ssl
        final SslConfig ssl = runtimeConfig.ssl;
        if (ssl != null) {
            setProperty(ssl.protocol, streamsProperties, SslConfigs.SSL_PROTOCOL_CONFIG);
            setProperty(ssl.provider, streamsProperties, SslConfigs.SSL_PROVIDER_CONFIG);
            setProperty(ssl.cipherSuites, streamsProperties, SslConfigs.SSL_CIPHER_SUITES_CONFIG);
            setProperty(ssl.enabledProtocols, streamsProperties, SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG);

            setTrustStoreConfig(ssl.truststore, streamsProperties);
            setKeyStoreConfig(ssl.keystore, streamsProperties);
            setKeyConfig(ssl.key, streamsProperties);

            setProperty(ssl.keymanagerAlgorithm, streamsProperties, SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG);
            setProperty(ssl.trustmanagerAlgorithm, streamsProperties, SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG);
            final Optional<String> eia = Optional.of(ssl.endpointIdentificationAlgorithm.orElse(""));
            setProperty(eia, streamsProperties, SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG);
            setProperty(ssl.secureRandomImplementation, streamsProperties,
                    SslConfigs.SSL_SECURE_RANDOM_IMPLEMENTATION_CONFIG);
        }

        return streamsProperties;
    }

    private static void setTrustStoreConfig(final TrustStoreConfig tsc, final Properties properties) {
        if (tsc != null) {
            setProperty(tsc.type, properties, SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG);
            setProperty(tsc.location, properties, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
            setProperty(tsc.password, properties, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG);
            setProperty(tsc.certificates, properties, SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG);
        }
    }

    private static void setKeyStoreConfig(final KeyStoreConfig ksc, final Properties properties) {
        if (ksc != null) {
            setProperty(ksc.type, properties, SslConfigs.SSL_KEYSTORE_TYPE_CONFIG);
            setProperty(ksc.location, properties, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG);
            setProperty(ksc.password, properties, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG);
            setProperty(ksc.key, properties, SslConfigs.SSL_KEYSTORE_KEY_CONFIG);
            setProperty(ksc.certificateChain, properties, SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG);
        }
    }

    private static void setKeyConfig(final KeyConfig kc, final Properties properties) {
        if (kc != null) {
            setProperty(kc.password, properties, SslConfigs.SSL_KEY_PASSWORD_CONFIG);
        }
    }

    private static <T> void setProperty(final Optional<T> property, final Properties properties, final String key) {
        setProperty(property, properties, key, Objects::toString);
    }

    private static <T> void setProperty(final Optional<T> property, final Properties properties, final String key,
            final Function<T, String> fn) {
        if (property.isPresent()) {
            properties.put(key, fn.apply(property.get()));
        }
    }

    private static String asString(final List<InetSocketAddress> addresses) {
        return addresses.stream()
                .map(KafkaStreamsProducer::toHostPort)
                .collect(Collectors.joining(","));
    }

    private static String toHostPort(final InetSocketAddress inetSocketAddress) {
        return inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort();
    }

    private static void waitForTopicsToBeCreated(final Admin adminClient, final Collection<String> topicsToAwait,
            final Duration timeout)
            throws InterruptedException {
        Set<String> lastMissingTopics = null;
        while (!shutdown) {
            try {
                final ListTopicsResult topics = adminClient.listTopics();
                final Set<String> existingTopics = topics.names().get(timeout.toMillis(), TimeUnit.MILLISECONDS);

                if (existingTopics.containsAll(topicsToAwait)) {
                    LOGGER.debug("All expected topics created: " + topicsToAwait);
                    return;
                } else {
                    final Set<String> missingTopics = new HashSet<>(topicsToAwait);
                    missingTopics.removeAll(existingTopics);

                    // Do not spam warnings - topics may take time to be created by an operator like Strimzi
                    if (missingTopics.equals(lastMissingTopics)) {
                        LOGGER.debug("Waiting for topic(s) to be created: " + missingTopics);
                    } else {
                        LOGGER.warn("Waiting for topic(s) to be created: " + missingTopics);
                        lastMissingTopics = missingTopics;
                    }
                }
            } catch (final ExecutionException | TimeoutException e) {
                LOGGER.error("Failed to get topic names from broker", e);
            } finally {
                Thread.sleep(1_000);
            }
        }
    }

    private static Properties getAdminClientConfig(final Properties properties) {
        final Properties adminClientConfig = new Properties(properties);
        // include other AdminClientConfig(s) that have been configured
        for (final String knownAdminClientConfig : AdminClientConfig.configNames()) {
            // give preference to admin.<propname> first
            if (properties.containsKey(StreamsConfig.ADMIN_CLIENT_PREFIX + knownAdminClientConfig)) {
                adminClientConfig.put(knownAdminClientConfig,
                        properties.getProperty(StreamsConfig.ADMIN_CLIENT_PREFIX + knownAdminClientConfig));
            } else if (properties.containsKey(knownAdminClientConfig)) {
                adminClientConfig.put(knownAdminClientConfig, properties.getProperty(knownAdminClientConfig));
            }
        }
        return adminClientConfig;
    }

    private static final class DurationToSecondsFunction implements Function<Duration, String> {

        private static final DurationToSecondsFunction INSTANCE = new DurationToSecondsFunction();

        @Override
        public String apply(final Duration d) {
            return String.valueOf(d.getSeconds());
        }
    }
}
