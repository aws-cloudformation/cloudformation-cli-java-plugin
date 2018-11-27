package com.aws.rpdk;

import com.aws.cfn.Action;

public class HandlerRequestImpl<T> implements HandlerRequest<T> {

    /**
     * The provisioning action to be performed.
     */
    private Action action;

    /**
     * Additional context for the request
     */
    private RequestContext requestContext;

    /**
     * A concrete instance of the resource type being provisioned
     */
    private T resourceModel;

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    public RequestContext getRequestContext() {
        return this.requestContext;
    }

    public void setRequestContext(final RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public T getResourceModel() {
        return this.resourceModel;
    }

    public void setResourceModel(final T resourceModel) {
        this.resourceModel = resourceModel;
    }

    public HandlerRequestImpl(
        final Action action,
        final T resourceModel,
        final RequestContext requestContext) {
        this.setAction(action);
        this.setRequestContext(requestContext);
        this.setResourceModel(resourceModel);
    }

    public HandlerRequestImpl() { }
}
