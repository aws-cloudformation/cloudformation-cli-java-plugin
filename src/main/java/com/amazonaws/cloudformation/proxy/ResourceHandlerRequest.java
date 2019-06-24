package com.amazonaws.cloudformation.proxy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This interface describes the request object for the provisioning request
 * passed to the implementor. It is transformed from an instance of
 * HandlerRequest by the LambdaWrapper to only items of concern
 *
 * @param <T> Type of resource model being provisioned
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ResourceHandlerRequest<T> {
    private String clientRequestToken;
    private T desiredResourceState;
    private T previousResourceState;
    private String logicalResourceIdentifier;
}
