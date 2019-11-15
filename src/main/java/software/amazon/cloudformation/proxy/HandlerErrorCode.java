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

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum HandlerErrorCode {

    /**
     * the customer tried perform an update to a property that is CreateOnly. Only
     * applicable to Update Handler (Terminal)
     */
    NotUpdatable(ExceptionMessages.NOT_UPDATABLE),

    /**
     * a generic exception caused by invalid input from the customer (Terminal)
     */
    InvalidRequest(ExceptionMessages.INVALID_REQUEST),

    /**
     * the customer has insufficient permissions to perform this action (Terminal)
     */
    AccessDenied(ExceptionMessages.ACCESS_DENIED),

    /**
     * the customer's provided credentials were invalid (Terminal)
     */
    InvalidCredentials(ExceptionMessages.INVALID_CREDENTIALS),

    /**
     * the specified resource already existed prior to the execution of the handler.
     * Only applicable to Create Handler (Terminal) Handlers MUST return this error
     * when duplicate creation requests are received.
     */
    AlreadyExists(ExceptionMessages.ALREADY_EXISTS),

    /**
     * the specified resource does not exist, or is in a terminal, inoperable, and
     * irrecoverable state (Terminal)
     */
    NotFound(ExceptionMessages.NOT_FOUND),

    /**
     * the resource is temporarily unable to be acted upon; for example, if the
     * resource is currently undergoing an operation and cannot be acted upon until
     * that operation is finished (Retriable)
     */
    ResourceConflict(ExceptionMessages.RESOURCE_CONFLICT),

    /**
     * the request was throttled by the downstream service (Retriable)
     */
    Throttling(ExceptionMessages.THROTTLING),

    /**
     * a non-transient resource limit was reached on the service side (Terminal)
     */
    ServiceLimitExceeded(ExceptionMessages.SERVICE_LIMIT_EXCEEDED),

    /**
     * the downstream resource failed to complete all of its ready state checks
     * (Retriable)
     */
    NotStabilized(ExceptionMessages.NOT_STABILIZED),

    /**
     * an exception from the downstream service that does not map to any other error
     * codes (Terminal)
     */
    GeneralServiceException(ExceptionMessages.GENERAL_SERVICE_EXCEPTION),

    /**
     * the downstream service returned an internal error, typically with a 5XX HTTP
     * Status code (Retriable)
     */
    ServiceInternalError(ExceptionMessages.SERVICE_INTERNAL_ERROR),

    /**
     * the request was unable to be completed due to networking issues, such as
     * failure to receive a response from the server (Retriable)
     */
    NetworkFailure(ExceptionMessages.NETWORK_FAILURE),

    /**
     * an unexpected error occurred within the handler, such as an NPE, etc.
     * (Terminal)
     */
    InternalFailure(ExceptionMessages.INTERNAL_FAILURE);

    @Getter
    private String message;
}
