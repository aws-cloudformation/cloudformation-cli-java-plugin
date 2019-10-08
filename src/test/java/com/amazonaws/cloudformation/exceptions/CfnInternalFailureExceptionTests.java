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

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.amazonaws.cloudformation.proxy.HandlerErrorCode;

import org.junit.jupiter.api.Test;

public class CfnInternalFailureExceptionTests {
    @Test
    public void cfnInternalFailureException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnInternalFailureException(new RuntimeException());
        });
    }

    @Test
    public void cfnInternalFailureException_singleArgConstructorHasMessage() {
        assertThatExceptionOfType(CfnInternalFailureException.class).isThrownBy(() -> {
            throw new CfnInternalFailureException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("Internal error");
    }

    @Test
    public void cfnInternalFailureException_noCauseGiven() {
        assertThatExceptionOfType(CfnInternalFailureException.class).isThrownBy(() -> {
            throw new CfnInternalFailureException();
        }).withNoCause().withMessageContaining("Internal error");
    }

    @Test
    public void cfnInternalFailureException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnInternalFailureException.class).isThrownBy(() -> {
            throw new CfnInternalFailureException(new RuntimeException());
        }).satisfies(exception -> assertEquals(HandlerErrorCode.InternalFailure, exception.getErrorCode()));
    }
}
