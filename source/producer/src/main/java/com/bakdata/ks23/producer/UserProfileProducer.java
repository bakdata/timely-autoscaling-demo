package com.bakdata.ks23.producer;

import com.bakdata.ks23.UserProfile;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import javax.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;

@ApplicationScoped
@Slf4j
public final class UserProfileProducer implements KafkaProducer {
    public static final String USER_PROFILE_CHANNEL = "user-profile";
    private final ZipCsvReader<UserProfile> zipCsvReader;
    private final ProducerConfig producerConfig;
    private final MutinyEmitter<Record<Integer, UserProfile>> emitter;

    public UserProfileProducer(final ProducerConfig producerConfig,
            @Channel(USER_PROFILE_CHANNEL) final MutinyEmitter<Record<Integer, UserProfile>> emitter) {
        this.producerConfig = producerConfig;
        this.zipCsvReader = new ZipCsvReader<>(UserProfile::new);
        this.emitter = emitter;
    }

    @Override
    public Multi<Void> produceRecords() {
        return Multi.createFrom()
                .items(this.zipCsvReader.readZippedCsv(this.producerConfig.zipPath(), "user_profile.csv"))
                .map(profile -> Record.of(profile.getUserId(), profile))
                .onItem()
                .transformToUni(this.emitter::send)
                .merge()
                .onSubscription().invoke(() -> log.info("Starting to produce user-profiles"))
                .onTermination().invoke(() -> log.info("Finished producing user-profiles"));
    }

    @Override
    public boolean enabled() {
        return this.producerConfig.userProfile().enabled();
    }

}
