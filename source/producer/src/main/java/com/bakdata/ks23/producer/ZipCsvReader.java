package com.bakdata.ks23.producer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

public class ZipCsvReader<T extends GenericRecord> {

    private static final Pattern CSV_PATTERN = Pattern.compile(",");
    public static final String NULL = "NULL";
    private final Supplier<T> recordSupplier;

    public ZipCsvReader(final Supplier<T> recordSupplier) {
        this.recordSupplier = recordSupplier;
    }

    public Stream<T> readZippedCsv(final Path archive, final String file) {
        if (!Files.exists(archive)) {
            throw new RuntimeException("Archive %s not found".formatted(archive));
        }

        try {
            final ZipFile zipFile = new ZipFile(archive);
            final ZipArchiveEntry entry = zipFile.getEntry(file);
            final InputStream inputStream = zipFile.getInputStream(entry);
            final BufferedReader bufferedInputStream = new BufferedReader(new InputStreamReader(inputStream));
            return bufferedInputStream.lines()
                    .skip(1) // skip header
                    .map(this::convert);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private T convert(final String rawCsvLine) {
        final String[] split = CSV_PATTERN.split(rawCsvLine);
        final T record = this.recordSupplier.get();
        final List<Field> fields = record.getSchema().getFields();
        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            final String value = i >= split.length ? NULL : split[i];
            final Schema schema = field.schema();
            if ((value.equals(NULL) || value.isBlank()) && schema.isNullable()) {
                continue;
            }

            final Object fieldValue = getFieldValue(schema, value);
            record.put(field.pos(), fieldValue);

        }
        return record;
    }

    private static Object getFieldValue(final Schema schema, final String value) {
        final Type type = schema.isUnion() ? findTypeInNullableUnion(schema) : schema.getType();

        if (type == Type.STRING) {
            return value;
        }

        if (type == Type.DOUBLE) {
            return Double.parseDouble(value);
        }

        final int parsedInt = Integer.parseInt(value);

        if (type == Type.INT || type == Type.LONG) {
            if (schema.getLogicalType() != null) {
                return Instant.ofEpochSecond(parsedInt);
            } else {
                return parsedInt;
            }
        } else if (type == Type.BOOLEAN) {
            return parsedInt == 0;
        }

        throw new IllegalArgumentException("value '%s' cannot be converted to type %s".formatted(value, type));
    }

    private static Type findTypeInNullableUnion(final Schema schema) {
        return schema.getTypes().stream()
                .filter(it -> it.getType() != Type.NULL)
                .findFirst()
                .orElseThrow()
                .getType();
    }

}
