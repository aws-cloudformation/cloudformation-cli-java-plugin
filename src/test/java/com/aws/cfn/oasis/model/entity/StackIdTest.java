package com.aws.cfn.oasis.model.entity;

import org.junit.Test;

import static org.junit.Assert.*;

public class StackIdTest {

    private static final String STACK_ID = "arn:aws:cloudformation:us-east-1:237885156816:stack/test/62141ec0-fc92-11e7-8395-5044334e0ab3";

    @Test
    public void testStackIdGetValue_validString_happyCase() {
        final StackId stackId = new StackId(STACK_ID);
        assertEquals(STACK_ID, stackId.getValue());
    }
}
