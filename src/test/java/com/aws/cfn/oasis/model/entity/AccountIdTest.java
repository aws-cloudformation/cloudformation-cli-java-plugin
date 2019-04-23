package com.aws.cfn.oasis.model.entity;

import org.junit.Test;

import static org.junit.Assert.*;

public class AccountIdTest {
    @Test
    public void testGetAccountIdValue_validString_happyCase() {
        final AccountId accountId = new AccountId("123456");
        assertEquals("123456", accountId.getValue());
    }
}
