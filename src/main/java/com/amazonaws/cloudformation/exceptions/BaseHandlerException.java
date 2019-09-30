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

import com.amazonaws.cloudformation.proxy.HandlerErrorCode;

public class BaseHandlerException extends RuntimeException {

    private static final long serialVersionUID = -1646136434112354328L;

    private HandlerErrorCode errorCode;

    BaseHandlerException(final Throwable cause,
                         final HandlerErrorCode errorCode) {
        super(null, cause);
        this.errorCode = errorCode;
    }

    BaseHandlerException(final String resourceTypeName,
                         final String resourceIdentifier,
                         final HandlerErrorCode errorCode) {
        this(resourceTypeName, resourceIdentifier, null, errorCode);
    }

    BaseHandlerException(final String resourceTypeName,
                         final String resourceIdentifier,
                         final Throwable cause,
                         final HandlerErrorCode errorCode) {
        this(String.format(ExceptionMessages.GENERIC, resourceTypeName, resourceIdentifier), cause, errorCode);
    }

    BaseHandlerException(final String message,
                         final Throwable cause,
                         final HandlerErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public HandlerErrorCode getErrorCode() {
        return errorCode;
    }
}
