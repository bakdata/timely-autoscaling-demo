package com.bakdata.ks23.producer;

import static com.bakdata.ks23.producer.AdFeatureProducer.AD_FEATURE_CHANNEL;
import static com.bakdata.ks23.producer.SampleProducer.SAMPLE_CHANNEL;
import static com.bakdata.ks23.producer.UserProfileProducer.USER_PROFILE_CHANNEL;

import com.bakdata.ks23.common.BootstrapConfig;
import io.smallrye.common.annotation.Identifier;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class ConfigFactory {
    private final BootstrapConfig bootstrapConfig;

    @Inject
    public ConfigFactory(final BootstrapConfig bootstrapConfig) {
        this.bootstrapConfig = bootstrapConfig;
    }

    @Produces
    @ApplicationScoped
    @Identifier(AD_FEATURE_CHANNEL)
    public Map<String, Object> adFeatureConfig() {
        return this.buildConfig(AD_FEATURE_CHANNEL);
    }

    @Produces
    @ApplicationScoped
    @Identifier(USER_PROFILE_CHANNEL)
    public Map<String, Object> userProfileConfig() {
        return this.buildConfig(USER_PROFILE_CHANNEL);
    }

    @Produces
    @ApplicationScoped
    @Identifier(SAMPLE_CHANNEL)
    public Map<String, Object> sampleConfig() {
        return this.buildConfig(SAMPLE_CHANNEL);
    }

    private Map<String, Object> buildConfig(final String channelName) {
        final Map<String, Object> config = new HashMap<>();
        config.put("topic", this.bootstrapConfig.extraOutputTopics().getOrDefault(channelName, channelName));
        config.put("value.serializer", io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        return config;
    }
}
