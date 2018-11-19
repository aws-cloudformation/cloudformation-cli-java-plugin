package com.aws.rpdk;

import com.google.gson.JsonObject;

public class RequestContext {

    private int invocation;
    private JsonObject callbackContext;

    /**
     * The Resource Type Name (e.g; "AWS::EC2::Instance") for this request
     */
    private String resourceType;

    /**
     * If the request was the result of a CloudWatchEvents re-invoke trigger the
     * CloudWatchEvents Rule name is stored to allow cleanup
     */
    private String cloudWatchEventsRuleName;

    /**
     * If the request was the result of a CloudWatchEvents re-invoke trigger the
     * CloudWatchEvents Trigger Id is stored to allow cleanup
     */
    private String cloudWatchEventsTargetId;

    public int getInvocation() {
        return this.invocation;
    }

    public void setInvocation(final int invocation) {
        this.invocation = invocation;
    }

    public JsonObject getCallbackContext() {
        return this.callbackContext;
    }

    public void setCallbackContext(final JsonObject callbackContext) {
        this.callbackContext = callbackContext;
    }

    public String getResourceType() {
        return this.resourceType;
    }

    public void setResourceType(final String resourceType) {
        this.resourceType = resourceType;
    }

    public String getCloudWatchEventsRuleName() {
        return this.cloudWatchEventsRuleName;
    }

    public void setCloudWatchEventsRuleName(final String cloudWatchEventsRuleName) {
        this.cloudWatchEventsRuleName = cloudWatchEventsRuleName;
    }

    public String getCloudWatchEventsTargetId() {
        return this.cloudWatchEventsTargetId;
    }

    public void setCloudWatchEventsTargetId(final String cloudWatchEventsTargetId) {
        this.cloudWatchEventsTargetId = cloudWatchEventsTargetId;
    }

    public RequestContext(final int invocation,
                          final JsonObject callbackContext,
                          final String resourceType) {
        this.setCallbackContext(callbackContext);
        this.setInvocation(invocation);
        this.setResourceType(resourceType);
    }

    public RequestContext() { }
}
