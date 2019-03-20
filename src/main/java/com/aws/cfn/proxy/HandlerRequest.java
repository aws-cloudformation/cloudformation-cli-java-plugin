package com.aws.cfn.proxy;

import com.aws.cfn.Action;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This interface describes the request object for the provisioning request
 */
@Data
@NoArgsConstructor
public class HandlerRequest<ResourceT, CallbackT> {
    private Action action;
    private String awsAccountId;
    private String bearerToken;
    private String nextToken;
    private String region;
    private String resourceType;
    private String resourceTypeVersion;
    private RequestData<ResourceT> requestData;
    private String stackId;
    private RequestContext<CallbackT> requestContext;
}
