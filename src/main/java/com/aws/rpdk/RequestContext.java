package com.aws.rpdk;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class RequestContext {
    /**
     * The number of times the handler has been invoked (including current)
     */
    private int invocation;

    /**
     * Custom context object to enable handlers to process re-invocation
     */
    private JsonObject callbackContext;

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
}
