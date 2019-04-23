package com.aws.cfn.oasis.model.entity;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Base class for strongly typing any different types of strings
 */
@Getter
abstract class BaseStringWrapper {
    private final String value;

    protected BaseStringWrapper(@Nonnull final String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(String.format("Value '%s' cannot must be nonnull and non-empty", value));
        }

        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o.getClass() != getClass()) return false;

        final BaseStringWrapper that = (BaseStringWrapper) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return getValue();
    }
}
