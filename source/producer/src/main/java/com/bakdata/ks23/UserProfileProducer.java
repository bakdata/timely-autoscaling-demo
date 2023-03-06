package com.bakdata.ks23;

import io.smallrye.mutiny.Multi;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public final class UserProfileProducer {
    private final ZipCsvReader<UserProfile> zipCsvReader;
    private final ProducerConfig producerConfig;


    public UserProfileProducer(final ProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
        this.zipCsvReader = new ZipCsvReader<>(UserProfile::new);
    }

    @Outgoing("user-profile-out")
    public Multi<UserProfile> produceUserProfiles() {
        if (!this.producerConfig.userProfile().enabled()) {
            return Multi.createFrom().empty();
        }
        return Multi.createFrom().items(this.zipCsvReader.readZippedCsv("data.zip", "user_profile.csv"));
    }

}
