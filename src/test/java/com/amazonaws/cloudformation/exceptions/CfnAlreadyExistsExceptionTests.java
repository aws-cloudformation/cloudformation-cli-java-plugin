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

import static junit.framework.Assert.*;

import com.amazonaws.cloudformation.proxy.HandlerErrorCode;

import org.junit.jupiter.api.Test;

public class CfnAlreadyExistsExceptionTests {
    private static final String EXPECTED_EXCEPTION_MESSAGE = "Resource of type 'AWS::Type::Resource' with identifier 'myId' already exists.";

    @Test
    public void resourceAlreadyExistsException_isCfnAlreadyExistsException() {
        try {
            throw new ResourceAlreadyExistsException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final CfnAlreadyExistsException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void resourceAlreadyExistsException_singleArgConstructorHasNoMessage() {
        try {
            throw new ResourceAlreadyExistsException(new RuntimeException());
        } catch (final ResourceAlreadyExistsException e) {
            assertNull(e.getMessage());
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void resourceAlreadyExistsException_noCauseGiven() {
        try {
            throw new ResourceAlreadyExistsException("AWS::Type::Resource", "myId");
        } catch (final ResourceAlreadyExistsException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNull(e.getCause());
        }
    }

    @Test
    public void resourceAlreadyExistsException_errorCodeIsAppropriate() {
        try {
            throw new ResourceAlreadyExistsException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final ResourceAlreadyExistsException e) {
            assertEquals(e.getErrorCode(), HandlerErrorCode.AlreadyExists);
        }
    }

    @Test
    public void cfnAlreadyExistsException_isBaseHandlerException() {
        try {
            throw new CfnAlreadyExistsException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final BaseHandlerException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void cfnAlreadyExistsException_singleArgConstructorHasNoMessage() {
        try {
            throw new CfnAlreadyExistsException(new RuntimeException());
        } catch (final CfnAlreadyExistsException e) {
            assertNull(e.getMessage());
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void cfnAlreadyExistsException_noCauseGiven() {
        try {
            throw new CfnAlreadyExistsException("AWS::Type::Resource", "myId");
        } catch (final CfnAlreadyExistsException e) {
            assertEquals(e.getMessage(), EXPECTED_EXCEPTION_MESSAGE);
            assertNull(e.getCause());
        }
    }

    @Test
    public void cfnAlreadyExistsException_errorCodeIsAppropriate() {
        try {
            throw new CfnAlreadyExistsException("AWS::Type::Resource", "myId", new RuntimeException());
        } catch (final CfnAlreadyExistsException e) {
            assertEquals(e.getErrorCode(), HandlerErrorCode.AlreadyExists);
        }
    }
}
