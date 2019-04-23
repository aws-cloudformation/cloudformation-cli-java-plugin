package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.proxy.HandlerErrorCode;
import com.aws.cfn.proxy.OperationStatus;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Signifies that CloudFormation should fail the current operation and begin rolling back
 * if appropriate
 */
@Value
public final class NextActionFail<CallbackT> implements NextAction<CallbackT> {
    @NonNull private final String failureReason;
    @NonNull private final HandlerErrorCode errorCode;

    @Override
    public void decorateResponse(@Nonnull final Response<CallbackT> response) {
        Objects.requireNonNull(response);

        response.setErrorCode(errorCode);
        response.setMessage(failureReason);
    }

    @Nonnull
    @Override
    public OperationStatus getOperationStatus() {
        return OperationStatus.FAILED;
    }
}
