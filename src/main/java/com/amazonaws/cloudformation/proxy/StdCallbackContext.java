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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.concurrent.ThreadSafe;

/**
 * StdCallbackContext provide a mechanism that automatically provides the
 * memoization for retention and callback of request, responses, stabilize
 * handles during handler invocations. During replay callbacks, this
 * automatically replays responses for different calls along the call graph
 * ensuring that we only execute the portions of the call graph that needs
 * execution and dedupe calls as needed.
 *
 * This is not a sophisticated class that does request inspection based call
 * result, it is primarily a function result memoization that is ensured that it
 * is invoked once. Attempts to call the function multiple times with different
 * arguments will yield the same result for the same call graph key for
 * {@link StdCallbackContext#request(String, Function)} and
 * {@link StdCallbackContext#response(String, BiFunction)}. For
 * {@link StdCallbackContext#stabilize(String, CallChain.Callback)}, only when
 * True is returned it is memoized.
 */
@ThreadSafe
@lombok.EqualsAndHashCode
@lombok.ToString
public class StdCallbackContext {

    /*
     * Uses a LinkedHashMap to preserve the order of calls within a set of
     * callGraphs. If things interleave in terms of entries then it means that the
     * context was being used in different threads.
     */
    private final Map<String, Object> callGraphContexts = Collections.synchronizedMap(new LinkedHashMap<>(10));

    @SuppressWarnings("unchecked")
    public <M, R> Function<M, R> request(String callGraph, Function<M, R> func) {
        return (m) -> (R) callGraphContexts.computeIfAbsent(callGraph + ".request", (ign) -> func.apply(m));
    }

    @SuppressWarnings("unchecked")
    public <R> R evictRequestRecord(String callGraph) {
        return (R) callGraphContexts.remove(callGraph + ".request");
    }

    @SuppressWarnings("unchecked")
    public <R, C, RT> BiFunction<R, C, RT> response(String callGraph, BiFunction<R, C, RT> func) {
        return (r, c) -> (RT) callGraphContexts.computeIfAbsent(callGraph + ".response", (ign) -> func.apply(r, c));
    }

    public Map<String, Object> getCallGraphs() {
        return Collections.unmodifiableMap(callGraphContexts);
    }

    <RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext>
        CallChain.Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean>
        stabilize(String callGraph, CallChain.Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean> callback) {
        return (request1, response1, client, model, context) -> {
            Boolean result = (Boolean) callGraphContexts.computeIfAbsent(callGraph + ".stabilize",
                (ign) -> callback.invoke(request1, response1, client, model, context) ? Boolean.TRUE : null);
            return result != null ? Boolean.TRUE : Boolean.FALSE;
        };
    }

    public int attempts(String callGraph) {
        return (Integer) callGraphContexts.computeIfAbsent(callGraph + ".attempts", (ign) -> 1);
    }

    public void attempts(String callGraph, int attempts) {
        callGraphContexts.put(callGraph + ".attempts", attempts);
    }
}
