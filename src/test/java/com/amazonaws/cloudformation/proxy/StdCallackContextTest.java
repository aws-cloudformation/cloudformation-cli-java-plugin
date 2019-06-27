/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.proxy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class StdCallackContextTest {

    private StdCallbackContext cxt = new StdCallbackContext();

    @Test
    public void multipleComputeDedupe() {
        int[] attempt = { 1 };
        Function<String, String> request = (in) -> {
            assertEquals(attempt[0]++, 1);
            return "Yup " + attempt[0] + in;
        };
        Function<String, String> memoized = cxt.request("aws:service", request);
        String req = memoized.apply("1");
        assertEquals("Yup 21", req);
        assertEquals("Yup 21", memoized.apply("same result diff arg"));
        assertEquals(attempt[0], 2);

        BiFunction<String, String, String> respone = (in1, in2) -> {
            assertEquals(attempt[0]++, 2);
            return "response " + attempt[0] + in1 + in2;
        };
        BiFunction<String, String, String> memoized2 = cxt.response("aws:service", respone);
        assertEquals("response 321", memoized2.apply("2", "1"));
        assertEquals("response 321", memoized2.apply("", "arg change same result"));
        assertEquals(attempt[0], 3);

        CallChain.Callback<String, String, String, String, StdCallbackContext,
            Boolean> waiter = (request1, response, client, model, context) -> attempt[0]++ < 5 ? false : true;

        CallChain.Callback<String, String, String, String, StdCallbackContext,
            Boolean> waiterMem = cxt.stabilize("aws:service", waiter);
        assertFalse(waiterMem.invoke(null, null, null, null, null));
        assertFalse(waiterMem.invoke(null, null, null, null, null));
        assertTrue(waiterMem.invoke(null, null, null, null, null));
        assertEquals(attempt[0], 6);
        assertTrue(waiterMem.invoke(null, null, null, null, null));
        assertTrue(waiterMem.invoke(null, null, null, null, null));
        assertEquals(attempt[0], 6);
    }
}
