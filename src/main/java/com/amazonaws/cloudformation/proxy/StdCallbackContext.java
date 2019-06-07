package com.amazonaws.cloudformation.proxy;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * StdCallbackContext provide a mechanism that automatically provides the facilities
 * for retention and callback of the request, responses, stabilize handles and more during
 * handler invocations. During replay callbacks, this automatically replays responses
 * for different calls along the call graph ensuring that we only execute the portions
 * of the call graph that needs execution and dedupe calls as needed.
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
        Boolean> wait(
            String callGraph,
            CallChain.Callback<RequestT,
                ResponseT,
                ClientT,
                ModelT,
                CallbackT,
                Boolean> callback) {
        return (request1, response1, client, model, context) ->
            (Boolean) callGraphContexts.computeIfAbsent(
                callGraph + ".stabilize",
                (ign) -> callback.invoke(request1, response1, client, model, context));
    }
}
