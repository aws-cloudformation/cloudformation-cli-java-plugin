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

public class CfnInvalidCredentialsExceptionTests {
    @Test
    public void cfnInvalidCredentialsException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnInvalidCredentialsException(new RuntimeException());
        });
    }

    @Test
    public void cfnInvalidCredentialsException_singleArgConstructorHasMessage() {
        assertThatExceptionOfType(CfnInvalidCredentialsException.class).isThrownBy(() -> {
            throw new CfnInvalidCredentialsException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("Invalid credentials");
    }

    @Test
    public void cfnInvalidCredentialsException_noCauseGiven() {
        assertThatExceptionOfType(CfnInvalidCredentialsException.class).isThrownBy(() -> {
            throw new CfnInvalidCredentialsException();
        }).withNoCause().withMessageContaining("Invalid credentials");
    }

    @Test
    public void cfnInvalidCredentialsException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnInvalidCredentialsException.class).isThrownBy(() -> {
            throw new CfnInvalidCredentialsException(new RuntimeException());
        }).satisfies(exception -> assertEquals(HandlerErrorCode.InvalidCredentials, exception.getErrorCode()));
    }
}
