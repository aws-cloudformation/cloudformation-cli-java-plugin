package com.aws.rpdk;

import com.aws.cfn.Action;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This interface describes the request object for the provisioning request
 * @param <T> Type of resource model being provisioned
 */
@Data
@NoArgsConstructor
public class HandlerRequest<T> {

    private static final String AWS_ACCOUNT_ID = "awsAccountId";
    private static final String BEARER_TOKEN = "bearerToken";
    private static final String CLIENT_REQUEST_TOKEN = "clientRequestToken";
    private static final String REGION = "region";
    private static final String RESOURCE_REQUEST_TYPE = "resourceRequestType";
    private static final String RESOURCE_TYPE = "resourceType";
    private static final String RESOURCE_TYPE_VERSION = "resourceTypeVersion";
    private static final String REQUEST_CONTEXT = "requestContext";
    private static final String REQUEST_DATA = "requestData";
    private static final String STACK_ID = "stackId";

    @JsonProperty(AWS_ACCOUNT_ID)
    private String awsAccountId;

    @JsonProperty(BEARER_TOKEN)
    private String bearerToken;

    @JsonProperty(CLIENT_REQUEST_TOKEN)
    private String clientRequestToken;

    @JsonProperty(REGION)
    private String region;

    @JsonProperty(RESOURCE_REQUEST_TYPE)
    private Action resourceRequestType;

    @JsonProperty(RESOURCE_TYPE)
    private String resourceType;

    @JsonProperty(RESOURCE_TYPE_VERSION)
    private String resourceTypeVersion;

    @JsonProperty(REQUEST_DATA)
    private RequestData<T> requestData;

    @JsonProperty(STACK_ID)
    private String stackId;

    @JsonProperty(REQUEST_CONTEXT)
    private RequestContext requestContext;
}
