package com.bakdata.ks23;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;

import com.bakdata.kafka.KafkaStreamsApplication;

public class StreamsApp extends KafkaStreamsApplication {

	public static void main(final String[] args) {
		startApplication(new StreamsApp(), args);
	}

	@Override
	public void buildTopology(final StreamsBuilder builder) {
		final KStream<String, String> input = builder.stream(this.getInputTopics());
		input.to(this.getOutputTopic());
	}

	@Override
	public String getUniqueAppId() {
		return "caller-" + this.getOutputTopic();
	}

}
