package com.aws.cfn.oasis.model;

import com.aws.cfn.oasis.model.nextaction.NextAction;
import lombok.NonNull;
import lombok.Value;

/**
 * Response from the handler implementation itself
 */
@Value
public class ProgressEvent<CallbackT> {
    @NonNull private final NextAction<CallbackT> nextAction;
}
