package com.aws.rpdk;

import com.aws.cfn.ProgressStatus;
import com.google.gson.JsonObject;

public class ProgressEvent {

    /**
     * The status indicates whether the handler has reached a terminal state or
     * is still computing and requires more time to complete
     */
    private ProgressStatus status;

    /**
     * The handler can (and should) specify a contextual information message which
     * can be shown to callers to indicate the nature of a progress transition
     * or callback delay; for example a message indicating "propagating to edge"
     */
    private String message;

    /**
     * The callback context is an arbitrary datum which the handler can return
     * in an InProgress event to allow the passing through of additional state
     * or metadata between subsequent retries; for example to pass through a
     * Resource identifier which can be used to continue polling for stabilization
     */
    private JsonObject callbackContext;

    /**
     * A callback will be scheduled with an initial delay of no less than
     * the number of minutes specified in the progress event. Set this
     * value to <= 0 to indicate no callback should be made.
     */
    private int callbackDelayMinutes;

    public ProgressStatus getStatus() {
        return this.status;
    }

    public void setStatus(final ProgressStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public  void setMessage(final String message) {
        this.message = message;
    }

    public JsonObject getCallbackContext() {
        return callbackContext;
    }

    public void setCallbackContext(final JsonObject callbackContext) {
        this.callbackContext = callbackContext;
    }

    public int getCallbackDelayMinutes() {
        return callbackDelayMinutes;
    }

    public void setCallbackDelayMinutes(final int callbackDelayMinutes) {
        this.callbackDelayMinutes = callbackDelayMinutes;
    }

    public ProgressEvent(
        final ProgressStatus status,
        final String message) {
        this.setStatus(status);
        this.setMessage(message);
    }

    public ProgressEvent(
        final ProgressStatus status,
        final String message,
        final JsonObject callbackContext,
        final int callbackDelayMinutes) {
        this.setStatus(status);
        this.setMessage(message);
        this.setCallbackContext(callbackContext);
        this.setCallbackDelayMinutes(callbackDelayMinutes);
    }
}
