package com.bakdata.ks23.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.spi.Converter;

public class BootstrapMapConverter implements Converter<Map<String, String>> {

    @Override
    public Map<String, String> convert(final String value) {
        if (value.isBlank()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(value.split(","))
                .flatMap(BootstrapMapConverter::convertToEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Stream<Entry<String, String>> convertToEntry(final String pair) {
        final String[] split = pair.split("=");
        if (split.length == 0) {
            return Stream.empty();
        }
        return Stream.of(Map.entry(split[0], split[1]));
    }

}
