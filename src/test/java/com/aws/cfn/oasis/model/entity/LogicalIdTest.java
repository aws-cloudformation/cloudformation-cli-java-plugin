package com.aws.cfn.oasis.model.entity;

import org.junit.Test;

import static org.junit.Assert.*;

public class LogicalIdTest {
    @Test
    public void testGetLogicalIdValue_validString_happyCase() {
        final LogicalId myResource = new LogicalId("MyResource");
        assertEquals("MyResource", myResource.getValue());
    }
}
