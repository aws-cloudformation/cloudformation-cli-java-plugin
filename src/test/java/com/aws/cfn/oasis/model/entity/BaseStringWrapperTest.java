package com.aws.cfn.oasis.model.entity;

import org.junit.Test;

import javax.annotation.Nonnull;

import static org.junit.Assert.*;

public class BaseStringWrapperTest {

    @Test
    public void testDummyStringWrapper_getValue_basicHappyCase() {
        final DummyStringTest testString = new DummyStringTest("Test");
        assertEquals("Test", testString.getValue());
    }

    @Test
    public void testDummyStringWrapper_toString_basicHappyCase() {
        final DummyStringTest testString = new DummyStringTest("Test");
        assertEquals("Test", testString.toString());
    }

    @Test
    public void testDummyStringWrapper_equalsSameString_expectTrue() {
        final DummyStringTest test1 = new DummyStringTest("Test");
        final DummyStringTest test2 = new DummyStringTest("Test");
        assertEquals(true, test1.equals(test2));
        assertEquals(true, test2.equals(test1));
    }

    @Test
    public void testDummyStringWrapper_equalsDifferentString_expectFalse() {
        final DummyStringTest test1 = new DummyStringTest("Test1");
        final DummyStringTest test2 = new DummyStringTest("Test2");
        assertEquals(false, test1.equals(test2));
        assertEquals(false, test2.equals(test1));
    }

    @Test
    public void testDummyStringWrapper_hashCodeIdenticalStrings_expectConsistent() {
        final DummyStringTest test1 = new DummyStringTest("Test");
        final DummyStringTest test2 = new DummyStringTest("Test");
        assertEquals(test1.hashCode(), test2.hashCode());
    }

    @Test
    public void testDummyStringWrapper_hashCodeDifferentStrings_expectDifferent() {
        final DummyStringTest test1 = new DummyStringTest("Test1");
        final DummyStringTest test2 = new DummyStringTest("Test2");
        assertNotEquals(test1.hashCode(), test2.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDummyStringWrapper_nullValue_expectIAEFromConstructor() {
        new DummyStringTest(null);
    }

    @Test
    public void testDummyStringWrapper_equalsDifferentSubclassSameString_expectNotEquals() {
        final DummyStringTest testType1 = new DummyStringTest("Test");
        final DifferentDummyStringTest testType2 = new DifferentDummyStringTest("Test");
        assertEquals(false, testType1.equals(testType2));
        assertEquals(false, testType2.equals(testType1));
    }

    private class DummyStringTest extends BaseStringWrapper {
        private DummyStringTest(@Nonnull final String value) {
            super(value);
        }
    }

    private class DifferentDummyStringTest extends BaseStringWrapper {
        private DifferentDummyStringTest(@Nonnull final String value) {
            super(value);
        }
    }
}
