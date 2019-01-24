package com.aws.cfn;

import com.aws.cfn.proxy.ProgressStatus;
import lombok.Data;

@Data
public class Response<T> {
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
     * The output resource instance populated by a READ/LIST for synchronous results
     * and by CREATE/UPDATE/DELETE for final response validation/confirmation
     */
    private T resourceModel;
}
