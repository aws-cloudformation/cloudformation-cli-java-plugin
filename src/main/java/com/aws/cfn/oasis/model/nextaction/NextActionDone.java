package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.proxy.OperationStatus;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Signifies that the handler is done operating (successfully) and the stack operation
 * is free to continue
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class NextActionDone<CallbackT> extends OptionalStackEventNextAction<CallbackT> {

    public NextActionDone(@Nullable final String message) {
        super(message);
    }

    @Nonnull
    @Override
    public OperationStatus getOperationStatus() {
        return OperationStatus.SUCCESS;
    }
}
