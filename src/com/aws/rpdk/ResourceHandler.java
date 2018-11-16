package com.aws.rpdk;

import com.aws.cfn.Action;

/**
 * NOTE: This is a duplicate interface declaration which needs to be removed as we
 * integrate the RPDK and pull in the actual interface for invocation
 */
public interface ResourceHandler<T> {

    ProgressEvent handleRequest(final HandlerRequest<T> request, Action action, final RequestContext context);

}
