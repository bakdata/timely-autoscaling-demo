package com.bakdata.ks23.metrics;

import com.bakdata.ks23.metrics.Metric.UniqueElements;
import com.bakdata.ks23.metrics.Stats.CounterStats;
import io.smallrye.mutiny.Uni;
import java.time.Instant;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Path("metrics")
@Slf4j
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {

    private final UniqueIdCounter usersCounter;
    private final UniqueIdCounter adsCounter;

    public MetricsResource(@Named("users") final UniqueIdCounter usersCounter,
            @Named("ads") final UniqueIdCounter adsCounter) {
        this.usersCounter = usersCounter;
        this.adsCounter = adsCounter;
    }

    @GET
    @Path("latest")
    public Uni<Metric> getLatestMetric() {
        return Uni.combine().all().unis(
                Uni.createFrom().item(this.usersCounter::reportUniqueIds),
                Uni.createFrom().item(this.adsCounter::reportUniqueIds)
        ).combinedWith((users, ads) -> new Metric(new UniqueElements(users, ads)));
    }


    @GET
    @Path("stats")
    public Uni<Stats> getStats() {
        return Uni.createFrom().item(() -> new Stats(fromCounter(this.usersCounter), fromCounter(this.adsCounter)));
    }

    private static CounterStats fromCounter(final UniqueIdCounter counter) {
        return new CounterStats(
                counter.getLastSeenPerKey().size(),
                counter.getUniqueElementsPerSecond().size(),
                Instant.ofEpochSecond(
                        counter.getUniqueElementsPerSecond().keySet().stream().min(Long::compareTo).orElse(0L)),
                Instant.ofEpochSecond(
                        counter.getUniqueElementsPerSecond().keySet().stream().max(Long::compareTo).orElse(0L))
        );
    }
}
