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
package com.amazonaws.cloudformation.proxy;

public enum HandlerErrorCode {

    /**
     * a generic exception caused by invalid input from the customer
     */
    InvalidRequest,

    /**
     * the customer has insufficient permissions to perform this action
     */
    AccessDenied,

    /**
     * the customer's provided credentials were invalid
     */
    InvalidCredentials,

    /**
     * the handler completed without making any modifying API calls (only applicable
     * to UpdateHandler)
     */
    NoOperationToPerform,

    /**
     * the customer tried perform an update to a property that is not updatable
     * (only applicable to UpdateHandler)
     */
    NotUpdatable,

    /**
     * the specified resource does not exist, or is in a terminal, inoperable, and
     * irrecoverable state
     */
    NotFound,

    /**
     * the resource is temporarily in an inoperable state
     */
    NotReady,

    /**
     * the request was throttled by the downstream service. Handlers SHOULD retry on
     * service throttling using exponential backoff in order to be resilient to
     * transient throttling.
     */
    Throttling,

    /**
     * a non-transient resource limit was reached on the service side
     */
    ServiceLimitExceeded,

    /**
     * the handler timed out waiting for the downstream service to perform an
     * operation
     */
    ServiceTimeout,

    /**
     * a generic exception from the downstream service
     */
    ServiceException,

    /**
     * the request was unable to be completed due to networking issues, such as
     * failure to receive a response from the server. Handlers SHOULD retry on
     * network failures using exponential backoff in order to be resilient to
     * transient issues.
     */
    NetworkFailure,

    /**
     * an unexpected error occurred within the handler, such as an NPE, etc.
     */
    InternalFailure,

    /**
     * a resource create request failed for an existing entity (only applicable to
     * CreateHandler) Handlers MUST return this error when duplicate creation
     * requests are received.
     */
    AlreadyExists
}
