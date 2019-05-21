package com.aws.cfn.proxy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProgressEvent<ResourceT, CallbackT> {
    /**
     * The status indicates whether the handler has reached a terminal state or
     * is still computing and requires more time to complete
     */
    private OperationStatus status;

    /**
     *  If OperationStatus is FAILED, an error code should be provided
     */
    private HandlerErrorCode errorCode;

    /**
     * The handler can (and should) specify a contextual information message which
     * can be shown to callers to indicate the nature of a progress transition
     * or callback delay; for example a message indicating "propagating to edge"
     */
    private String message;

    /**
     * The callback context is an arbitrary datum which the handler can return
     * in an IN_PROGRESS event to allow the passing through of additional state
     * or metadata between subsequent retries; for example to pass through a
     * Resource identifier which can be used to continue polling for stabilization
     */
    private CallbackT callbackContext;

    /**
     * A callback will be scheduled with an initial delay of no less than
     * the number of minutes specified in the progress event. Set this
     * value to <= 0 to indicate no callback should be made.
     */
    private int callbackDelayMinutes;

    /**
     * The output resource instance populated by a READ/LIST for synchronous results
     * and by CREATE/UPDATE/DELETE for final response validation/confirmation
     */
    private ResourceT resourceModel;
}
