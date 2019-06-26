package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.Action;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This POJO is for the test entrypoint that bypasses the wrapper for direct testing.
 * @param <ModelT> Type of resource model being provisioned
 * @param <CallbackT> Type of callback data to be passed on re-invocation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ResourceHandlerTestPayload<ModelT, CallbackT> {
    private Credentials credentials;
    private Action action;
    private ResourceHandlerRequest<ModelT> request;
    private CallbackT callbackContext;
}
