package com.amazonaws.cloudformation.proxy;

public enum HandlerErrorCode {

    /**
     * the customer tried perform an update to a property that is CreateOnly. Only applicable to Update Handler (Terminal)
     */
    NotUpdatable,

    /**
     * a generic exception caused by invalid input from the customer (Terminal)
     */
    InvalidRequest,

    /**
     * the customer has insufficient permissions to perform this action (Terminal)
     */
    AccessDenied,

    /**
     * the customer's provided credentials were invalid (Terminal)
     */
    InvalidCredentials,

    /**
     * the specified resource already existed prior to the execution of the handler. Only applicable to Create Handler (Terminal)
     * Handlers MUST return this error when duplicate creation requests are received.
     */
    AlreadyExists,

    /**
     * the specified resource does not exist, or is in a terminal, inoperable, and irrecoverable state (Terminal)
     */
    NotFound,

    /**
     * the resource is temporarily unable to be acted upon; for example, if the resource is currently undergoing an operation and cannot be acted upon until that operation is finished (Retriable)
     */
    ResourceConflict,

    /**
     * the request was throttled by the downstream service (Retriable)
     */
    Throttling,

    /**
     * a non-transient resource limit was reached on the service side (Terminal)
     */
    ServiceLimitExceeded,

    /**
     * the downstream resource failed to complete all of its ready state checks (Retriable)
     */
    NotStabilized,

    /**
     * an exception from the downstream service that does not map to any other error codes (Terminal)
     */
    GeneralServiceException,

    /**
     * the downstream service returned an internal error, typically with a 5XX HTTP Status code (Retriable)
     */
    ServiceInternalError,

    /**
     * the request was unable to be completed due to networking issues, such as failure to receive a response from the server (Retriable)
     */
    NetworkFailure,

    /**
     * an unexpected error occurred within the handler, such as an NPE, etc. (Terminal)
     */
    InternalFailure

}
