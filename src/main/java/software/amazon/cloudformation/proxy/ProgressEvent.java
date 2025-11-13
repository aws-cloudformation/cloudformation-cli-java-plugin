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
package software.amazon.cloudformation.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.hook.HookAnnotation;

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
     * If OperationStatus is FAILED or IN_PROGRESS, an error code should be provided
     */
    private HandlerErrorCode errorCode;

    /**
     * The handler can (and should) specify a contextual information message which
     * can be shown to callers to indicate the nature of a progress transition or
     * callback delay; for example a message indicating "propagating to edge"
     */
    private String message;

    /**
     * The optional data to be returned by a hook handler as part of the invocation
     * result. This field is ignored for resource handlers.
     */
    private String result;

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
     * The output resource instance populated by a READ for synchronous results and
     * by CREATE/UPDATE/DELETE for final response validation/confirmation
     */
    private ResourceT resourceModel;

    /**
     * The output resource instances populated by a LIST for synchronous results
     */
    private List<ResourceT> resourceModels;

    /**
     * The token used to request additional pages of resources for a LIST operation
     */
    private String nextToken;

    /**
     * The optional list of HookAnnotation objects that, if used by a CloudFormation
     * Hook, contain additional, user-defined metadata and information on the
     * results of a hook's evaluation.
     *
     * Note: this field is ignored for resource handlers.
     */
    private List<HookAnnotation> annotations;

    /**
     * Convenience method for constructing a FAILED response
     *
     * @param e actual service exception
     * @param handlerErrorCode error code to return to CloudFormation
     * @param <ResourceT> The type for the resource model
     * @param <CallbackT> the type for the callback context
     * @return {@link ProgressEvent} failed status code
     */
    public static <ResourceT, CallbackT>
        ProgressEvent<ResourceT, CallbackT>
        defaultFailureHandler(final Throwable e, final HandlerErrorCode handlerErrorCode) {

        return failed(null, null, handlerErrorCode, e.getMessage());
    }

    public static <ResourceT, CallbackT>
        ProgressEvent<ResourceT, CallbackT>
        failed(ResourceT model, CallbackT cxt, HandlerErrorCode code, String message) {

        ProgressEvent<ResourceT, CallbackT> event = progress(model, cxt);
        event.setStatus(OperationStatus.FAILED);
        event.setErrorCode(code);
        event.setMessage(message);
        return event;
    }

    /**
     * Convenience method for constructing a IN_PROGRESS response
     *
     * @param callbackContext callback context
     * @param callbackDelaySeconds how much time to wait before calling back the
     *            handler
     * @param resourceModel the actual resource model state
     * @param <ResourceT> The type for the resource model
     * @param <CallbackT> the type for the callback context
     * @return {@link ProgressEvent} with {@link OperationStatus#IN_PROGRESS} with
     *         delay
     */
    public static <ResourceT, CallbackT>
        ProgressEvent<ResourceT, CallbackT>
        defaultInProgressHandler(final CallbackT callbackContext, final int callbackDelaySeconds, final ResourceT resourceModel) {

        return ProgressEvent.<ResourceT, CallbackT>builder().callbackContext(callbackContext)
            .callbackDelaySeconds(callbackDelaySeconds).resourceModel(resourceModel).status(OperationStatus.IN_PROGRESS).build();
    }

    public static <ResourceT, CallbackT> ProgressEvent<ResourceT, CallbackT> progress(ResourceT model, CallbackT cxt) {

        return ProgressEvent.<ResourceT, CallbackT>builder().callbackContext(cxt).resourceModel(model)
            .status(OperationStatus.IN_PROGRESS).build();
    }

    /**
     * Convenience method for constructing a SUCCESS response
     *
     * @param resourceModel the current resource model state
     * @param <ResourceT> The type for the resource model
     * @param <CallbackT> the type for the callback context
     * @return {@link ProgressEvent} with {@link OperationStatus#SUCCESS} indicating
     *         successful completion for operation
     */
    public static <ResourceT,
        CallbackT> ProgressEvent<ResourceT, CallbackT> defaultSuccessHandler(final ResourceT resourceModel) {

        return success(resourceModel, null);
    }

    public static <ResourceT, CallbackT> ProgressEvent<ResourceT, CallbackT> success(ResourceT model, CallbackT cxt) {

        ProgressEvent<ResourceT, CallbackT> event = progress(model, cxt);
        event.setStatus(OperationStatus.SUCCESS);
        return event;
    }

    public static <ResourceT,
        CallbackT> ProgressEvent<ResourceT, CallbackT> success(ResourceT model, CallbackT cxt, String message) {
        return ProgressEvent.<ResourceT, CallbackT>builder().resourceModel(model).callbackContext(cxt).message(message)
            .status(OperationStatus.SUCCESS).build();
    }

    public static <ResourceT,
        CallbackT> ProgressEvent<ResourceT, CallbackT> success(ResourceT model, CallbackT cxt, String message, String result) {
        return ProgressEvent.<ResourceT, CallbackT>builder().resourceModel(model).callbackContext(cxt).message(message)
            .result(result).status(OperationStatus.SUCCESS).build();
    }

    public static <ResourceT, CallbackT>
        ProgressEvent<ResourceT, CallbackT>
        success(ResourceT model, CallbackT cxt, String message, List<HookAnnotation> annotations) {
        return ProgressEvent.<ResourceT, CallbackT>builder().resourceModel(model).callbackContext(cxt).message(message)
            .annotations(annotations).status(OperationStatus.SUCCESS).build();
    }

    public static <ResourceT, CallbackT>
        ProgressEvent<ResourceT, CallbackT>
        success(ResourceT model, CallbackT cxt, String message, String result, List<HookAnnotation> annotations) {
        return ProgressEvent.<ResourceT, CallbackT>builder().resourceModel(model).callbackContext(cxt).message(message)
            .result(result).annotations(annotations).status(OperationStatus.SUCCESS).build();
    }

    public ProgressEvent<ResourceT, CallbackT>
        onSuccess(Function<ProgressEvent<ResourceT, CallbackT>, ProgressEvent<ResourceT, CallbackT>> func) {
        return (status != null && status == OperationStatus.SUCCESS) ? func.apply(this) : this;
    }

    @JsonIgnore
    public boolean isFailed() {
        return status == OperationStatus.FAILED;
    }

    @JsonIgnore
    public boolean isInProgress() {
        return status == OperationStatus.IN_PROGRESS;
    }

    @JsonIgnore
    public boolean canContinueProgress() {
        return status == OperationStatus.IN_PROGRESS && callbackDelaySeconds == 0;
    }

    public ProgressEvent<ResourceT, CallbackT>
        then(Function<ProgressEvent<ResourceT, CallbackT>, ProgressEvent<ResourceT, CallbackT>> func) {
        return canContinueProgress() ? func.apply(this) : this;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return status == OperationStatus.SUCCESS;
    }

    @JsonIgnore
    public boolean isInProgressCallbackDelay() {
        return status == OperationStatus.IN_PROGRESS && callbackDelaySeconds > 0;
    }

}
