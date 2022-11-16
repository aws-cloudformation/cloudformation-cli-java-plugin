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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class CfnUnsupportedTargetExceptionTests {
    private static final String HOOK_TYPE = "AWS::Hook::Type";
    private static final String UNSUPPORTED_TYPE = "AWS::Service::Resource";
    private static final String STATUS = "unsupported target";
    private static final String ERROR_MESSAGE = "something wrong";

    @Test
    public void cfnUnsupportedTargetException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnUnsupportedTargetException(HOOK_TYPE, UNSUPPORTED_TYPE, new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining(HOOK_TYPE).withMessageContaining(UNSUPPORTED_TYPE)
            .withMessageContaining(STATUS);
    }

    @Test
    public void cfnUnsupportedTargetException_singleArgsConstructorHasNoMessage() {
        assertThatExceptionOfType(CfnUnsupportedTargetException.class).isThrownBy(() -> {
            throw new CfnUnsupportedTargetException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessage(null);
    }

    @Test
    public void cfnUnsupportedTargetException_noCauseGiven() {
        assertThatExceptionOfType(CfnUnsupportedTargetException.class).isThrownBy(() -> {
            throw new CfnUnsupportedTargetException(HOOK_TYPE, UNSUPPORTED_TYPE);
        }).withNoCause().withMessageContaining(HOOK_TYPE).withMessageContaining(UNSUPPORTED_TYPE).withMessageContaining(STATUS);
    }

    @Test
    public void cfnUnsupportedTargetException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnUnsupportedTargetException.class).isThrownBy(() -> {
            throw new CfnUnsupportedTargetException(new RuntimeException());
        }).satisfies(exception -> Assertions.assertEquals(HandlerErrorCode.UnsupportedTarget, exception.getErrorCode()));
    }

    @Test
    public void cfnUnsupportedTargetException_errorMessage() {
        assertThatExceptionOfType(CfnUnsupportedTargetException.class).isThrownBy(() -> {
            throw new CfnUnsupportedTargetException(new RuntimeException(ERROR_MESSAGE));
        }).satisfies(exception -> Assertions.assertEquals(ERROR_MESSAGE, exception.getMessage()));
    }

}
