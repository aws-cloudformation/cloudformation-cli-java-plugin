package com.aws.cfn.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TerminalException extends RuntimeException {

    protected static final String FAILURE_MODE_KEY = "failureMode";
    protected static final String MSG_KEY = "msg";
    protected static final String CAUSE_KEY = "cause";
    protected static final String SERVICE_KEY = "service";
    protected static final String RESOURCE_KEY = "resourceType";

    private static final long serialVersionUID = -1646136434112354328L;

    @JsonProperty(FAILURE_MODE_KEY) private final FailureMode failureMode;
    @JsonProperty(SERVICE_KEY) private String service;
    @JsonProperty(RESOURCE_KEY) private String resource;

    public TerminalException(final Throwable cause,
                             final FailureMode failureMode) {
        super(null, cause);
        this.failureMode = failureMode;
    }

    public TerminalException(final String customerFacingErrorMessage,
                             final FailureMode failureMode) {
        super(customerFacingErrorMessage);
        this.failureMode = failureMode;
    }

    public TerminalException(final String customerFacingErrorMessage,
                             final Throwable cause,
                             final FailureMode failureMode) {
        super(customerFacingErrorMessage, cause);
        this.failureMode = failureMode;
    }

    @JsonCreator
    private TerminalException(
        @JsonProperty(MSG_KEY) final String errorMessage,
        @JsonProperty(CAUSE_KEY) final Throwable cause,
        @JsonProperty(FAILURE_MODE_KEY) final FailureMode failureMode,
        @JsonProperty(SERVICE_KEY) final String service,
        @JsonProperty(RESOURCE_KEY) final String resource) {

        super(errorMessage, cause);
        this.failureMode = failureMode;
        this.service = service;
        this.resource = resource;
    }
}
