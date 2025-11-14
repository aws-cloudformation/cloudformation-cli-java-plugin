/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.proxy.hook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HookProgressEvent<CallbackT> {
    /**
     * The bearerToken is used to report progress back to CloudFormation and is
     * passed back to CloudFormation
     */
    private String clientRequestToken;

    /**
     * The status indicates whether the handler has reached a terminal state or is
     * still computing and requires more time to complete
     */
    private HookStatus hookStatus;

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
     * of seconds specified in the progress event.
     */
    private int callbackDelaySeconds;

    /**
     * The optional data to be returned by hook handler as part of the invocation
     * result.
     */
    private String result;

    /**
     * The optional list of HookAnnotation objects that, if used by a CloudFormation
     * Hook, contain additional, user-defined metadata and information on the
     * results of a hook's evaluation.
     */
    private List<HookAnnotation> annotations;

    /**
     * Convenience method for constructing a FAILED response
     *
     * @param e actual service exception
     * @param handlerErrorCode error code to return to CloudFormation
     * @param <CallbackT> the type for the callback context
     * @return {@link HookProgressEvent} failed status code
     */
    public static <CallbackT> HookProgressEvent<CallbackT> defaultFailureHandler(final Throwable e,
                                                                                 final HandlerErrorCode handlerErrorCode) {
        return failed(null, handlerErrorCode, e.getMessage(), null);
    }

    public static <
        CallbackT> HookProgressEvent<CallbackT> failed(CallbackT cxt, HandlerErrorCode code, String message, String data) {
        return HookProgressEvent.<CallbackT>builder().callbackContext(cxt).errorCode(code).message(message).result(data)
            .hookStatus(HookStatus.FAILED).build();
    }

    /**
     * Convenience method for constructing a IN_PROGRESS response
     *
     * @param cxt callback context
     * @param delaySeconds how much time to wait before calling back the handler
     * @param <CallbackT> The type for the callback context
     * @return {@link HookProgressEvent} with {@link HookStatus#IN_PROGRESS}
     */
    public static <CallbackT> HookProgressEvent<CallbackT> defaultProgressHandler(CallbackT cxt, int delaySeconds) {
        return progress(cxt, null, null, delaySeconds);
    }

    public static <
        CallbackT> HookProgressEvent<CallbackT> progress(CallbackT cxt, String message, String data, int delaySeconds) {
        return HookProgressEvent.<CallbackT>builder().callbackContext(cxt).callbackDelaySeconds(delaySeconds).message(message)
            .result(data).hookStatus(HookStatus.IN_PROGRESS).build();
    }

    /**
     * Convenience method for constructing a SUCCESS response
     *
     * @param <CallbackT> The type for the callback context
     * @return {@link HookProgressEvent} with {@link HookStatus#SUCCESS} indicating
     *         successful completion for operation
     */
    public static <CallbackT> HookProgressEvent<CallbackT> defaultCompleteHandler() {
        return complete(null, null, null);
    }

    public static <CallbackT> HookProgressEvent<CallbackT> complete(CallbackT cxt, String message, String data) {
        return HookProgressEvent.<CallbackT>builder().callbackContext(cxt).message(message).result(data)
            .hookStatus(HookStatus.SUCCESS).build();
    }

    public HookProgressEvent<CallbackT> onComplete(Function<HookProgressEvent<CallbackT>, HookProgressEvent<CallbackT>> func) {
        return (hookStatus != null && hookStatus == HookStatus.SUCCESS) ? func.apply(this) : this;
    }

    @JsonIgnore
    public boolean isFailed() {
        return hookStatus == HookStatus.FAILED;
    }

    @JsonIgnore
    public boolean isInProgress() {
        return hookStatus == HookStatus.IN_PROGRESS;
    }

    @JsonIgnore
    public boolean canContinueProgress() {
        return hookStatus == HookStatus.IN_PROGRESS && callbackDelaySeconds == 0;
    }

    public HookProgressEvent<CallbackT> then(Function<HookProgressEvent<CallbackT>, HookProgressEvent<CallbackT>> func) {
        return canContinueProgress() ? func.apply(this) : this;
    }

    @JsonIgnore
    public boolean isComplete() {
        return hookStatus == HookStatus.SUCCESS;
    }

    @JsonIgnore
    public boolean isInProgressCallbackDelay() {
        return hookStatus == HookStatus.IN_PROGRESS && callbackDelaySeconds > 0;
    }
}
