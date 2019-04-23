package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.proxy.OperationStatus;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * A generic type of NextAction that has a message associated with it.  This message,
 * if present, will be published to the stack events
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public abstract class OptionalStackEventNextAction<CallbackT> implements NextAction<CallbackT> {
    @Nullable private String message;

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    @Override
    public void decorateResponse(@Nonnull final Response<CallbackT> response) {
        Objects.requireNonNull(response);
        if (message != null) response.setMessage(message);
    }

    @Nonnull
    @Override
    public abstract OperationStatus getOperationStatus();
}
