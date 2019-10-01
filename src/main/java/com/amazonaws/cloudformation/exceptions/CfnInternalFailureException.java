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

public class CfnInternalFailureException extends BaseHandlerException {

    private static final long serialVersionUID = -1646136434112354328L;
    private static final HandlerErrorCode ERROR_CODE = HandlerErrorCode.InternalFailure;

    public CfnInternalFailureException() {
        this(null);
    }

    public CfnInternalFailureException(final Throwable cause) {
        super(ExceptionMessages.INTERNAL_FAILURE, cause, ERROR_CODE);
    }
}
