package com.bakdata.ks23;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
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
    private final Supplier<T> supplier;

    public ZipCsvReader(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public Stream<T> readZippedCsv(final String archive, final String file) {
        try {
            final URL resource = ZipCsvReader.class.getClassLoader().getResource(archive);
            if (resource == null) {
                throw new RuntimeException("Archive %s not found".formatted(archive));
            }
            final ZipFile zipFile = new ZipFile(resource.getPath());
            final ZipArchiveEntry entry = zipFile.getEntry(file);
            final InputStream inputStream = zipFile.getInputStream(entry);
            final BufferedReader bufferedInputStream = new BufferedReader(new InputStreamReader(inputStream));
            return bufferedInputStream.lines().skip(1).map(this::convert);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private T convert(final String rawCsvLine) {
        final String[] split = CSV_PATTERN.split(rawCsvLine);
        final T record = this.supplier.get();
        final List<Field> fields = record.getSchema().getFields();
        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            final String value = i >= split.length ? NULL : split[i];
            final Schema schema = field.schema();
            if ((value.equals(NULL) || value.isBlank()) && schema.isNullable()) {
                continue;
            }

            final Type type = schema.isUnion() ? findTypeInNullableUnion(schema) : schema.getType();

            if (type == Type.STRING) {
                record.put(field.pos(), value);
                continue;
            }

            if (type == Type.DOUBLE) {
                record.put(field.pos(), Double.parseDouble(value));
                continue;
            }

            final int parsedInt = Integer.parseInt(value);

            if (type == Type.INT || type == Type.LONG) {
                if (schema.getLogicalType() != null) {
                    record.put(field.pos(), Instant.ofEpochSecond(parsedInt));
                } else {
                    record.put(field.pos(), parsedInt);
                }
            } else if (type == Type.BOOLEAN) {
                record.put(field.pos(), parsedInt == 0);
            }

        }
        return record;
    }

    private static Type findTypeInNullableUnion(final Schema schema) {
        return schema.getTypes().stream()
                .filter(it -> it.getType() != Type.NULL)
                .findFirst()
                .orElseThrow()
                .getType();
    }

}
