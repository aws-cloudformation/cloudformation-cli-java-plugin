package com.aws.cfn.oasis.model.entity;

import javax.annotation.Nonnull;

/**
 * The ARN of a stack
 */
public class StackId extends BaseStringWrapper {
    public StackId(@Nonnull final String value) {
        super(value);
    }
}
