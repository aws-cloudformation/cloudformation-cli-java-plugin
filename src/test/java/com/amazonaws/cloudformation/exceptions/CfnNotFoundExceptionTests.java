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
import static junit.framework.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.amazonaws.cloudformation.proxy.HandlerErrorCode;

import org.junit.jupiter.api.Test;

public class CfnNotFoundExceptionTests {
    private static final String EXPECTED_EXCEPTION_MESSAGE = "Resource of type 'AWS::Type::Resource' with identifier 'myId' was not found.";

    @Test
    public void resourceNotFoundException_isCfnNotFoundException() {
        try {
            throw new ResourceNotFoundException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final CfnNotFoundException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void resourceNotFoundException_singleArgConstructorHasNoMessage() {
        try {
            throw new ResourceNotFoundException(new RuntimeException());
        } catch (final ResourceNotFoundException e) {
            assertNull(e.getMessage());
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void resourceNotFoundException_noCauseGiven() {
        try {
            throw new ResourceNotFoundException("AWS::Type::Resource", "myId");
        } catch (final ResourceNotFoundException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNull(e.getCause());
        }
    }

    @Test
    public void resourceNotFoundException_errorCodeIsAppropriate() {
        try {
            throw new ResourceNotFoundException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final ResourceNotFoundException e) {
            assertEquals(e.getErrorCode(), HandlerErrorCode.NotFound);
        }
    }

    @Test
    public void cfnNotFoundException_isBaseHandlerException() {
        try {
            throw new CfnNotFoundException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final BaseHandlerException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void cfnNotFoundException_singleArgConstructorHasNoMessage() {
        try {
            throw new CfnNotFoundException(new RuntimeException());
        } catch (final CfnNotFoundException e) {
            assertNull(e.getMessage());
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void cfnNotFoundException_noCauseGiven() {
        try {
            throw new CfnNotFoundException("AWS::Type::Resource", "myId");
        } catch (final CfnNotFoundException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNull(e.getCause());
        }
    }

    @Test
    public void cfnNotFoundException_errorCodeIsAppropriate() {
        try {
            throw new CfnNotFoundException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final CfnNotFoundException e) {
            assertEquals(e.getErrorCode(), HandlerErrorCode.NotFound);
        }
    }
}
