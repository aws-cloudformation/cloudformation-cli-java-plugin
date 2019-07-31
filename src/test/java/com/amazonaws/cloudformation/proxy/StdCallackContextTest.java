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

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;

import com.amazonaws.cloudformation.resource.Serializer;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.json.JSONObject;
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

    @lombok.Data
    @lombok.ToString
    @lombok.EqualsAndHashCode
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SimplePOJO {
        private String name;
        private String lastThing;
    }

    @Test
    public void testSimpleSerDeser() throws Exception {
        LinkedHashMap<String, Object> callGraphs = new LinkedHashMap<>();
        callGraphs.put("string", "string");
        callGraphs.put("integer", 1);
        callGraphs.put("pojo", new SimplePOJO("name", "did this last"));
        StdCallbackContext cxt = new StdCallbackContext();
        cxt.setCallGraphs(callGraphs);

        Serializer serializer = new Serializer();
        JSONObject serialized = serializer.serialize(cxt);
        StdCallbackContext deserialized = serializer.deserialize(serialized.toString(), new TypeReference<StdCallbackContext>() {
        });
        assertThat(cxt).isEqualTo(deserialized);
    }

    @Test
    public void testListAndMapSerDeser() throws Exception {
        LinkedHashMap<String, Object> callGraphs = new LinkedHashMap<>();
        callGraphs.put("string", "string");
        callGraphs.put("integer", 1);
        callGraphs.put("pojo", new SimplePOJO("name", "did this last"));
        callGraphs.put("list", new ArrayList<>(Arrays.asList("one", "two", "three")));
        Map<String, String> map = new HashMap<>();
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        callGraphs.put("map", map);

        StdCallbackContext cxt = new StdCallbackContext();
        cxt.setCallGraphs(callGraphs);
        Serializer serializer = new Serializer();
        JSONObject serialized = serializer.serialize(cxt);
        StdCallbackContext deserialized = serializer.deserialize(serialized.toString(), new TypeReference<StdCallbackContext>() {
        });
        assertThat(deserialized).isEqualTo(cxt);
    }

    @Test
    public void testListOfLists() throws Exception {
        LinkedHashMap<String, Object> callGraphs = new LinkedHashMap<>();
        ArrayList<List<String>> listOfLists = new ArrayList<>(2);
        listOfLists.add(new ArrayList<>(Arrays.asList("one", "two")));
        listOfLists.add(new ArrayList<>(Arrays.asList("three")));
        callGraphs.put("listOfLists", listOfLists);

        StdCallbackContext cxt = new StdCallbackContext();
        cxt.setCallGraphs(callGraphs);
        Serializer serializer = new Serializer();
        JSONObject serialized = serializer.serialize(cxt);
        StdCallbackContext deserialized = serializer.deserialize(serialized.toString(), new TypeReference<StdCallbackContext>() {
        });
        assertThat(deserialized).isEqualTo(cxt);
    }

    @Test
    public void testListOfPOJOs() throws Exception {
        LinkedHashMap<String, Object> callGraphs = new LinkedHashMap<>();
        ArrayList<SimplePOJO> simplePOJOS = new ArrayList<>(2);
        simplePOJOS.add(new SimplePOJO("one", "there we are"));
        simplePOJOS.add(new SimplePOJO("two", "here again"));
        callGraphs.put("simplePOJOS", simplePOJOS);

        StdCallbackContext cxt = new StdCallbackContext();
        cxt.setCallGraphs(callGraphs);
        Serializer serializer = new Serializer();
        JSONObject serialized = serializer.serialize(cxt);
        StdCallbackContext deserialized = serializer.deserialize(serialized.toString(), new TypeReference<StdCallbackContext>() {
        });
        assertThat(deserialized).isEqualTo(cxt);
    }

    @Test
    public void testListOfMixed() throws Exception {
        LinkedHashMap<String, Object> callGraphs = new LinkedHashMap<>();
        ArrayList<Object> simplePOJOS = new ArrayList<>(2);
        simplePOJOS.add(new SimplePOJO("one", "there we are"));
        simplePOJOS.add("break me");
        simplePOJOS.add(new SimplePOJO("two", "here again"));
        callGraphs.put("simplePOJOS", simplePOJOS);

        StdCallbackContext cxt = new StdCallbackContext();
        cxt.setCallGraphs(callGraphs);
        Serializer serializer = new Serializer();
        JSONObject serialized = serializer.serialize(cxt);
        StdCallbackContext deserialized = serializer.deserialize(serialized.toString(), new TypeReference<StdCallbackContext>() {
        });
        assertThat(deserialized).isEqualTo(cxt);
    }

    // @Test
    public void testSerDeser() throws Exception {
        LinkedHashMap<String, Object> callGraphs = new LinkedHashMap<>();
        ListRulesRequest listRulesRequest = ListRulesRequest.builder().limit(10).namePrefix("blah").build();
        ListRulesResponse listRulesResponse = ListRulesResponse.builder().rules(Rule.builder().arn("rule1")
            .description("rule1 description").managedBy("amazon").eventPattern("AWS::EC2::Instance").build()).build();
        callGraphs.put("cloudwatcheventss:ListRules.request", listRulesRequest);
        callGraphs.put("cloudwatcheventss:ListRules.response", listRulesResponse);
        callGraphs.put("cloudwatcheventss:ListRules.attempt", 1);

        PutEventsRequest putEventsRequest = PutEventsRequest.builder().entries(
            PutEventsRequestEntry.builder().detailType("AWS::EC2::Instance").resources("i-1234567890", "i-0123456789").build())
            .build();
        PutEventsResponse putEventsResponse = PutEventsResponse.builder()
            .entries(PutEventsResultEntry.builder().eventId("event-1").build()).build();
        callGraphs.put("cloudwatcheventss:PutEvents.request", putEventsRequest);
        callGraphs.put("cloudwatcheventss:PutEvents.response", putEventsResponse);
        callGraphs.put("cloudwatcheventss:PutEvents.attempt", 2);

        StdCallbackContext cxt = new StdCallbackContext();
        cxt.setCallGraphs(callGraphs);
        Serializer serializer = new Serializer();
        JSONObject serialized = serializer.serialize(cxt);
        StdCallbackContext deserialized = serializer.deserialize(serialized.toString(), new TypeReference<StdCallbackContext>() {
        });
        assertThat(cxt).isEqualTo(deserialized);
    }
}
