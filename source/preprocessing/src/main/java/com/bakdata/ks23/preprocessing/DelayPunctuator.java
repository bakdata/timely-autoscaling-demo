package com.bakdata.ks23.preprocessing;

import com.bakdata.ks23.FullSample;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

public class DelayPunctuator implements Punctuator {
    private final long delayMillis;
    private final KeyValueStore<Long, FullSample> timestampStore;
    private final ProcessorContext<byte[], FullSample> context;

    private long from = 0;

    public DelayPunctuator(final long delayMillis, final KeyValueStore<Long, FullSample> timestampStore,
            final ProcessorContext<byte[], FullSample> context) {
        this.delayMillis = delayMillis;
        this.timestampStore = timestampStore;
        this.context = context;
    }

    @Override
    public void punctuate(final long timestamp) {
        final long to = timestamp - this.delayMillis;
        try (final KeyValueIterator<Long, FullSample> range = this.timestampStore.range(this.from, to)) {

            range.forEachRemaining(keyValue -> {
                this.context.forward(new Record<>(null, keyValue.value, keyValue.key));
                this.timestampStore.delete(keyValue.key);
            });
        }
        this.from = to;
    }
}
