package com.aws.cfn.oasis.model;

import com.aws.cfn.oasis.model.iteration.Iteration;
import com.aws.cfn.proxy.HandlerErrorCode;
import com.aws.cfn.proxy.OperationStatus;
import lombok.Data;

import java.util.List;

@Data
public class Response<CallbackT> {
    /**
     * The bearerToken is used to report progress back to CloudFormation and is passed
     * back to CloudFormation
     */
    private String bearerToken;

    /**
     * The errorCode is used to report granular failures back to CloudFormation
     */
    private HandlerErrorCode errorCode;

    /**
     * The operationStatus indicates whether the handler has reached a terminal state or
     * is still computing and requires more time to complete
     */
    private OperationStatus operationStatus;

    /**
     * The handler can (and should) specify a contextual information message which
     * can be shown to callers to indicate the nature of a progress transition
     * or callback delay; for example a message indicating "propagating to edge"
     */
    private String message;

    /**
     * The output resource instance populated by a READ/LIST for synchronous results
     * and by CREATE/UPDATE/DELETE for final response validation/confirmation
     */
    private List<Iteration> outputModel;

    /**
     * In the event of an IN_PROGRESS status, the provider specifies an amount of time
     * after which CloudFormation will reinvoke the handler if no SUCCESS signal has been
     * received
     */
    private int callbackDelayMinutes;

    /**
     * In the event of a callback, the provider can supply a context to pass along to its
     * next invocation
     */
    private CallbackT callbackContext;
}
