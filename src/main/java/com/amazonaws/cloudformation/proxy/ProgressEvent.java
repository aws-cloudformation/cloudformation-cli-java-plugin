package com.amazonaws.cloudformation.proxy;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProgressEvent<ResourceT, CallbackT> {
    /**
     * The status indicates whether the handler has reached a terminal state or is
     * still computing and requires more time to complete
     */
    private OperationStatus status;

    /**
     * If OperationStatus is FAILED, an error code should be provided
     */
    private HandlerErrorCode errorCode;

    /**
     * The handler can (and should) specify a contextual information message which
     * can be shown to callers to indicate the nature of a progress transition or
     * callback delay; for example a message indicating "propagating to edge"
     */
    private String message;

    /**
     * The callback context is an arbitrary datum which the handler can return in an
     * IN_PROGRESS event to allow the passing through of additional state or
     * metadata between subsequent retries; for example to pass through a Resource
     * identifier which can be used to continue polling for stabilization
     */
    private CallbackT callbackContext;

    /**
     * A callback will be scheduled with an initial delay of no less than the number
     * of seconds specified in the progress event. Set this value to <= 0 to
     * indicate no callback should be made.
     */
    private int callbackDelaySeconds;

    /**
     * The output resource instance populated by a READ for synchronous results and
     * by CREATE/UPDATE/DELETE for final response validation/confirmation
     */
    private ResourceT resourceModel;

    /**
     * The output resource instances populated by a LIST for synchronous results
     */
    private List<ResourceT> resourceModels;

    /**
     * Convenience method for constructing a FAILED response
     */
    public static <ResourceT, CallbackT>
           ProgressEvent<ResourceT, CallbackT>
           defaultFailureHandler(final Throwable e, final HandlerErrorCode handlerErrorCode) {

        return ProgressEvent.<ResourceT, CallbackT>builder().errorCode(handlerErrorCode).message(e.getMessage())
            .status(OperationStatus.FAILED).build();
    }

    /**
     * Convenience method for constructing a SUCCESS response
     */
    public static <ResourceT, CallbackT>
           ProgressEvent<ResourceT, CallbackT>
           defaultInProgressHandler(final CallbackT callbackContext,
                                    final int callbackDelaySeconds,
                                    final ResourceT resourceModel) {

        return ProgressEvent.<ResourceT, CallbackT>builder().callbackContext(callbackContext)
            .callbackDelaySeconds(callbackDelaySeconds).resourceModel(resourceModel).status(OperationStatus.IN_PROGRESS).build();
    }

    /**
     * Convenience method for constructing a SUCCESS response
     */
    public static <ResourceT, CallbackT>
           ProgressEvent<ResourceT, CallbackT>
           defaultSuccessHandler(final ResourceT resourceModel) {

        return ProgressEvent.<ResourceT, CallbackT>builder().resourceModel(resourceModel).status(OperationStatus.SUCCESS).build();
    }
}
