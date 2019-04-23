package com.aws.cfn.oasis.model.entity;

import javax.annotation.Nonnull;

/**
 * Logical id of a resource
 */
public class LogicalId extends BaseStringWrapper {
    public LogicalId(@Nonnull final String value) {
        super(value);
    }
}
