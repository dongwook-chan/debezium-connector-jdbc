/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.jdbc.type.debezium;

import java.sql.Types;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;

import io.debezium.connector.jdbc.ValueBindDescriptor;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.type.AbstractTimestampType;
import io.debezium.connector.jdbc.type.Type;
import io.debezium.time.ZonedTimestamp;

/**
 * An implementation of {@link Type} for {@link ZonedTimestamp} values.
 *
 * @author Chris Cranford
 */
public class DebeziumZonedTimestampType extends AbstractTimestampType {

    public static final DebeziumZonedTimestampType INSTANCE = new DebeziumZonedTimestampType();
    public static final String POSITIVE_INFINITY = "infinity";
    public static final String NEGATIVE_INFINITY = "-infinity";

    @Override
    public String[] getRegistrationKeys() {
        return new String[]{ ZonedTimestamp.SCHEMA_NAME };
    }

    @Override
    public String getDefaultValueBinding(DatabaseDialect dialect, Schema schema, Object value) {
        return dialect.getFormattedTimestampWithTimeZone((String) value);
    }

    protected Object convertInfinityTimestampValue(Object value) {
    }

    public List<ValueBindDescriptor> bind(int index, Schema schema, Object value, boolean oracleInifinityConverterEnable) {
        if (value == null) {
            return List.of(new ValueBindDescriptor(index, null));
        }
        if (value instanceof String) {

            if (POSITIVE_INFINITY.equals(value) || NEGATIVE_INFINITY.equals(value)) {
                return infinityTimestampValue(index, value);
            }

            convertInfinityTimestampValue(value);

            return normalTimestampValue(index, value);
        }

        throw new ConnectException(String.format("Unexpected %s value '%s' with type '%s'", getClass().getSimpleName(),
                value, value.getClass().getName()));
    }

    @Override
    public List<ValueBindDescriptor> bind(int index, Schema schema, Object value) {
        return bind(index, schema, value, false);
    }

    protected List<ValueBindDescriptor> infinityTimestampValue(int index, Object value) {
        final ZonedDateTime zdt;

        if (POSITIVE_INFINITY.equals(value)) {
            zdt = ZonedDateTime.parse(getDialect().getTimestampPositiveInfinityValue(), ZonedTimestamp.FORMATTER);
        }
        else {
            zdt = ZonedDateTime.parse(getDialect().getTimestampNegativeInfinityValue(), ZonedTimestamp.FORMATTER);
        }

        return List.of(new ValueBindDescriptor(index, zdt.toOffsetDateTime(), getJdbcBindType()));
    }

    protected List<ValueBindDescriptor> normalTimestampValue(int index, Object value) {

        final ZonedDateTime zdt;
        zdt = ZonedDateTime.parse((String) value, ZonedTimestamp.FORMATTER).withZoneSameInstant(getDatabaseTimeZone().toZoneId());

        return List.of(new ValueBindDescriptor(index, zdt.toOffsetDateTime(), getJdbcBindType()));
    }

    protected int getJdbcBindType() {
        return getJdbcType();
    }

    @Override
    protected int getJdbcType() {
        return Types.TIMESTAMP_WITH_TIMEZONE;
    }

}
