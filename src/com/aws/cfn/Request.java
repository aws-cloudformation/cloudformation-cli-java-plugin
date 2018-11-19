package com.aws.cfn;

import com.aws.rpdk.ResourceModel;

public class Request {
    ResourceModel resource;

    int invocation;

    public ResourceModel getResource() {
        return resource;
    }

    public void setResource(final ResourceModel resource) {
        this.resource = resource;
    }

    public int getInvocation() {
        return invocation;
    }

    public void setInvocation(final int invocation) {
        this.invocation = invocation;
    }

    public Request(final ResourceModel resource,
                   final int invocation) {
        this.resource = resource;
        this.invocation = invocation;
    }

    public Request() {
    }
}
