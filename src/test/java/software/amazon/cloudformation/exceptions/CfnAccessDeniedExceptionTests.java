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

public class CfnAccessDeniedExceptionTests {
    @Test
    public void cfnAccessDeniedException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnAccessDeniedException("CreateThing", new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("CreateThing")
            .withMessageContaining("Access denied");
    }

    @Test
    public void cfnAccessDeniedException_singleArgConstructorHasNoMessage() {
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> {
            throw new CfnAccessDeniedException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessage(null);
    }

    @Test
    public void cfnAccessDeniedException_noCauseGiven() {
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> {
            throw new CfnAccessDeniedException("CreateThing");
        }).withNoCause().withMessageContaining("CreateThing").withMessageContaining("Access denied");
    }

    @Test
    public void cfnAccessDeniedException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> {
            throw new CfnAccessDeniedException(new RuntimeException());
        }).satisfies(exception -> assertEquals(HandlerErrorCode.AccessDenied, exception.getErrorCode()));
    }

    @Test
    public void cfnAccessDeniedException_errorMessage() {
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> {
            throw new CfnAccessDeniedException(new RuntimeException("something wrong"));
        }).satisfies(exception -> assertEquals("something wrong", exception.getMessage()));
    }
}
