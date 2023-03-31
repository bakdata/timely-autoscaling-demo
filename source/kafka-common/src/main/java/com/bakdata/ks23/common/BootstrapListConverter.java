package com.bakdata.ks23.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.microprofile.config.spi.Converter;

public class BootstrapListConverter implements Converter<List<String>> {
    @Override
    public List<String> convert(final String value) {
        if (value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(",")).toList();
    }

}
