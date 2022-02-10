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

public class CfnHandlerInternalFailureExceptionTests {
    @Test
    public void cfnHandlerInternalFailureException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnHandlerInternalFailureException(new RuntimeException());
        });
    }

    @Test
    public void cfnHandlerInternalFailureException_singleArgConstructorHasMessage() {
        assertThatExceptionOfType(CfnHandlerInternalFailureException.class).isThrownBy(() -> {
            throw new CfnHandlerInternalFailureException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("Internal error occurred in the handler");
    }

    @Test
    public void cfnHandlerInternalFailureException_noCauseGiven() {
        assertThatExceptionOfType(CfnHandlerInternalFailureException.class).isThrownBy(() -> {
            throw new CfnHandlerInternalFailureException();
        }).withNoCause().withMessageContaining("Internal error occurred in the handler");
    }

    @Test
    public void cfnHandlerInternalFailureException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnHandlerInternalFailureException.class).isThrownBy(() -> {
            throw new CfnHandlerInternalFailureException(new RuntimeException());
        }).satisfies(exception -> Assertions.assertEquals(HandlerErrorCode.HandlerInternalFailure, exception.getErrorCode()));
    }
}
