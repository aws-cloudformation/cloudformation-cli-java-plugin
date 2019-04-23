package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.proxy.OperationStatus;

import javax.annotation.Nonnull;

/**
 * A response from the handler to signal back to CloudFormation what the next
 * action to take in the operation will be (i.e. fail, continue, etc).
 */
public interface NextAction<CallbackT> {
    @Nonnull OperationStatus getOperationStatus();

    void decorateResponse(@Nonnull final Response<CallbackT> response);
}
