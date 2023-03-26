package com.bakdata.ks23;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
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
    public Multi<Record<Integer, UserProfile>> produceUserProfiles() {
        if (!this.producerConfig.userProfile().enabled()) {
            return Multi.createFrom().empty();
        }
        return Multi.createFrom()
                .items(this.zipCsvReader.readZippedCsv(this.producerConfig.zipPath(), "user_profile.csv"))
                .map(profile -> Record.of(profile.getUserId(), profile));
    }

}
