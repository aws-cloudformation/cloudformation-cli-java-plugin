package com.amazonaws.cloudformation.proxy;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * StdCallbackContext provide a mechanism that automatically provides the memoization
 * for retention and callback of request, responses, stabilize handles during
 * handler invocations. During replay callbacks, this automatically replays responses
 * for different calls along the call graph ensuring that we only execute the portions
 * of the call graph that needs execution and dedupe calls as needed.
 *
 * This is not a sophisticated class that does request inspection based call result, it
 * is primarily a function result memoization that is ensured that it is invoked once. Attempts
 * to call the function multiple times with different arguments will yield the same result
 * for the same call graph key for {@link StdCallbackContext#request(String, Function)}
 * and {@link StdCallbackContext#response(String, BiFunction)}. For
 * {@link StdCallbackContext#stabilize(String, CallChain.Callback)}, only when True is
 * returned it is memoized.
 */
@ThreadSafe
@lombok.EqualsAndHashCode
@lombok.ToString
public class StdCallbackContext {

    private final ConcurrentMap<String, Object> callGraphContexts =
        new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <M, R> Function<M, R> request(String callGraph, Function<M, R> func) {
        return (m) ->
            (R) callGraphContexts.computeIfAbsent(
                callGraph + ".request", (ign) -> func.apply(m));
    }

    @SuppressWarnings("unchecked")
    public <R, C, RT> BiFunction<R, C, RT> response(String callGraph, BiFunction<R, C, RT> func) {
        return (r, c) ->
            (RT) callGraphContexts.computeIfAbsent(
                callGraph + ".response", (ign) -> func.apply(r, c));
    }

    <RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext>
    CallChain.Callback<RequestT,
        ResponseT,
        ClientT,
        ModelT,
        CallbackT,
        Boolean> stabilize(
            String callGraph,
            CallChain.Callback<RequestT,
                ResponseT,
                ClientT,
                ModelT,
                CallbackT,
                Boolean> callback) {
        return (request1, response1, client, model, context) -> {
            Boolean result = (Boolean) callGraphContexts.computeIfAbsent(
                callGraph + ".stabilize",
                (ign) ->
                    callback.invoke(request1, response1, client, model, context) ?
                        Boolean.TRUE : null
                );
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
