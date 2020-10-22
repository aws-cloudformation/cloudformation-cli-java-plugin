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
package software.amazon.cloudformation.exceptions;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class CfnNotFoundExceptionTests {
    @Test
    public void resourceNotFoundException_isCfnNotFoundException() {
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> {
            throw new ResourceNotFoundException("AWS::Type::Resource", "myId", new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("AWS::Type::Resource").withMessageContaining("myId")
            .withMessageContaining("not found");
    }

    @Test
    public void resourceNotFoundException_singleArgConstructorHasNoMessage() {
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> {
            throw new ResourceNotFoundException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessage(null);
    }

    @Test
    public void resourceNotFoundException_noCauseGiven() {
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> {
            throw new ResourceNotFoundException("AWS::Type::Resource", "myId");
        }).withNoCause().withMessageContaining("AWS::Type::Resource").withMessageContaining("myId")
            .withMessageContaining("not found");
    }

    @Test
    public void resourceNotFoundException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> {
            throw new ResourceNotFoundException(new RuntimeException());
        }).satisfies(exception -> assertEquals(HandlerErrorCode.NotFound, exception.getErrorCode()));
    }

    @Test
    public void cfnNotFoundException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnNotFoundException("AWS::Type::Resource", "myId", new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("AWS::Type::Resource").withMessageContaining("myId")
            .withMessageContaining("not found");
    }

    @Test
    public void cfnNotFoundException_singleArgConstructorHasNoMessage() {
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> {
            throw new CfnNotFoundException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessage(null);
    }

    @Test
    public void cfnNotFoundException_noCauseGiven() {
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> {
            throw new CfnNotFoundException("AWS::Type::Resource", "myId");
        }).withNoCause().withMessageContaining("AWS::Type::Resource").withMessageContaining("myId")
            .withMessageContaining("not found");
    }

    @Test
    public void cfnNotFoundException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> {
            throw new CfnNotFoundException(new RuntimeException());
        }).satisfies(exception -> assertEquals(HandlerErrorCode.NotFound, exception.getErrorCode()));
    }

    @Test
    public void cfnNotFoundException_errorMessage() {
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> {
            throw new CfnNotFoundException(new RuntimeException("something wrong"));
        }).satisfies(exception -> assertEquals("something wrong", exception.getMessage()));
    }
}
