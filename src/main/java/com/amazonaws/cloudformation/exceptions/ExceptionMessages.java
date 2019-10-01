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
package com.amazonaws.cloudformation.exceptions;

final class ExceptionMessages {
    static final String ACCESS_DENIED = "Access denied for operation '%s'.";
    static final String ALREADY_EXISTS = "Resource of type '%s' with identifier '%s' already exists.";
    static final String GENERAL_SERVICE_EXCEPTION = "Error occurred during operation '%s'.";
    static final String INTERNAL_FAILURE = "Internal error occurred.";
    static final String INVALID_CREDENTIALS = "Invalid credentials provided.";
    static final String INVALID_REQUEST = "Invalid request provided: %s";
    static final String NETWORK_FAILURE = "Network failure occurred during operation '%s'.";
    static final String NOT_FOUND = "Resource of type '%s' with identifier '%s' was not found.";
    static final String NOT_STABILIZED = "Resource of type '%s' with identifier '%s' did not stabilize.";
    static final String NOT_UPDATABLE = "Resource of type '%s' with identifier '%s' is not updatable with parameters provided.";
    static final String RESOURCE_CONFLICT = "Resource of type '%s' with identifier '%s' has a conflict. Reason: %s.";
    static final String SERVICE_INTERNAL_ERROR = "Internal error reported from downstream service during operation '%s'.";
    static final String SERVICE_LIMIT_EXCEEDED = "Limit exceeded for resource of type '%s'. Reason: %s";
    static final String THROTTLING = "Rate exceeded for operation '%s'.";

    private ExceptionMessages() {
    }
}
