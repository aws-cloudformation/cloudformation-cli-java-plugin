package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.oasis.model.iteration.Iteration;
import com.aws.cfn.proxy.OperationStatus;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * Represents the response when the iterations are all produced and the handler's work
 * is done.  It contains the list of iterations that will be applied sequentially during
 * the update operation
 */
@Value
@EqualsAndHashCode
public class NextActionApplyIterations<CallbackT> implements NextAction<CallbackT> {
    @NonNull private final List<Iteration> iterations;

    @Nonnull
    @Override
    public OperationStatus getOperationStatus() {
        return OperationStatus.SUCCESS;
    }

    @Override
    public void decorateResponse(@Nonnull final Response<CallbackT> response) {
        Objects.requireNonNull(response);
        response.setOutputModel(iterations);
    }
}
