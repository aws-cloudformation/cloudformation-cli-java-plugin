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
    static final String ALREADY_EXISTS = "Resource of type '%s' with identifier '%s' already exists.";
    static final String GENERIC = "Resource of type '%s' with identifier '%s' encountered an error.";
    static final String NOT_FOUND = "Resource of type '%s' with identifier '%s' was not found.";

    private ExceptionMessages() {
    }
}
