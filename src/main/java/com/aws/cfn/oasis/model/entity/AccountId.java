package com.aws.cfn.oasis.model.entity;

import javax.annotation.Nonnull;

/**
 * A customer's account id
 */
public class AccountId extends BaseStringWrapper {
    public AccountId(@Nonnull final String value) {
        super(value);
    }
}
