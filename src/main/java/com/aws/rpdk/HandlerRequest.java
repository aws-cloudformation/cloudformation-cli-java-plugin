package com.aws.rpdk;

import com.aws.cfn.Action;

/**
 * This interface describes the request object for the provisioning request
 * @param <T> Type of resource model being provisioned
 */
public interface HandlerRequest<T> {

    Action getAction();
    void setAction(Action action);

    T getResourceModel();
    void setResourceModel(T resourceModel);

    RequestContext getRequestContext();
    void setRequestContext(RequestContext context);
}
