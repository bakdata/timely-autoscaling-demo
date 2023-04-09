package com.bakdata.ks23.metrics;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Getter(AccessLevel.PACKAGE)
@Slf4j
public class UniqueIdCounter {

    private static final int ELEMENT_BUFFER = 10;
    private static final long COMPACT_INTERVAL = 30L;
    private final Map<Long, Long> uniqueElementsPerSecond;
    private final Map<Integer, Long> lastSeenPerKey;
    private final long cacheTimeout;
    private final long slidingWindow;

    public UniqueIdCounter(final long cacheTimeout, final long slidingWindow, final ScheduledExecutorService service) {
        this.uniqueElementsPerSecond = new ConcurrentHashMap<>((int) cacheTimeout + ELEMENT_BUFFER);
        this.lastSeenPerKey = new ConcurrentHashMap<>();
        this.cacheTimeout = cacheTimeout;
        this.slidingWindow = slidingWindow;
        service.scheduleAtFixedRate(this::compactMaps, 0L, COMPACT_INTERVAL, TimeUnit.SECONDS);
        service.scheduleAtFixedRate(this::cleanElements, 0L, 1L, TimeUnit.SECONDS);
    }

    public void observeId(final int id) {
        final long currentSecond = currentSecond();
        final Long idLastSeen = this.lastSeenPerKey.get(id);
        if (idLastSeen == null || (currentSecond - idLastSeen) >= this.cacheTimeout) {
            this.uniqueElementsPerSecond.merge(currentSecond, 1L, Long::sum);
        } else {
            this.uniqueElementsPerSecond.putIfAbsent(currentSecond, 0L);
        }
        this.lastSeenPerKey.put(id, currentSecond);
    }

    public double reportUniqueIds() {
        // this might not be totally accurate because
        // - new elements might be written in the meantime
        // - or old values are not yet discarded,
        // but it doesn't really matter in this case as we report the average anyway
        final Collection<Long> values = this.uniqueElementsPerSecond.values();
        return values.stream().collect(Collectors.averagingLong(Long::longValue));
    }

    private void cleanElements() {
        this.uniqueElementsPerSecond.remove(currentSecond() - this.slidingWindow);
    }

    private void compactMaps() {
        final long currentSecond = currentSecond();
        this.uniqueElementsPerSecond.keySet().removeIf(second -> second <= currentSecond - this.slidingWindow);
        this.lastSeenPerKey.values().removeIf(second -> second <= currentSecond - this.cacheTimeout);
    }


    private static long currentSecond() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
