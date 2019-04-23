package com.aws.cfn.oasis.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemplateTest {
    @Test
    public void testTemplateToString_validInput() {
        final Template template = new Template("{}");
        assertEquals("{}", template.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testCreateTemplate_nullInput_assertConstructorFails() {
        new Template(null);
    }
}