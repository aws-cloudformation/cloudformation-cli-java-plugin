package com.aws.rpdk;

import com.aws.cfn.Action;
import lombok.Data;

/**
 * This interface describes the request object for the provisioning request
 * @param <T> Type of resource model being provisioned
 */
@Data
public abstract class HandlerRequest<T> {

    private Action action;
    private String region;
    private AWSCredentials awsCredentials;
    private T resourceModel;
    private RequestContext requestContext;
}
