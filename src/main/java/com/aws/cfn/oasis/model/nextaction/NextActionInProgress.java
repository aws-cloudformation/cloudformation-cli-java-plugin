package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.proxy.OperationStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * Signifies that the next step to be made by CloudFormation is to reinvoke the handler (unless
 * a subsequent SUCCESS signal is received before then)
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public final class NextActionInProgress<CallbackT> extends OptionalStackEventNextAction<CallbackT> {
    private final int callbackDelayMinutes;
    @Nullable private final CallbackT callbackContext;

    public NextActionInProgress(final int callbackDelayMinutes, @Nullable final CallbackT callbackContext, @Nullable final String eventMessage) {
        super(eventMessage);
        this.callbackContext = callbackContext;
        this.callbackDelayMinutes = callbackDelayMinutes;
    }

    public NextActionInProgress(final int callbackDelayMinutes, @Nullable final String message) {
        this(callbackDelayMinutes, null, message);
    }

    public NextActionInProgress(final int callbackDelayMinutes, @Nullable final CallbackT callbackContext) {
        this(callbackDelayMinutes, callbackContext, null);
    }

    public NextActionInProgress(final int callbackDelayMinutes) {
        this(callbackDelayMinutes, null, null);
    }

    public Optional<CallbackT> getCallbackContext() {
        return Optional.ofNullable(callbackContext);
    }

    @Override
    public void decorateResponse(@Nonnull final Response<CallbackT> response) {
        Objects.requireNonNull(response);

        super.decorateResponse(response);
        response.setCallbackDelayMinutes(callbackDelayMinutes);
        if (callbackContext != null) response.setCallbackContext(callbackContext);
    }

    @Nonnull
    @Override
    public OperationStatus getOperationStatus() {
        return OperationStatus.IN_PROGRESS;
    }
}
