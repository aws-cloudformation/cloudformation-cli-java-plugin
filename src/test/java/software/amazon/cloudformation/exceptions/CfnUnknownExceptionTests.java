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

public class CfnUnknownExceptionTests {
    @Test
    public void cfnUnknownException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnUnknownException(new RuntimeException());
        });
    }

    @Test
    public void cfnUnknownException_singleArgConstructorHasMessage() {
        assertThatExceptionOfType(CfnUnknownException.class).isThrownBy(() -> {
            throw new CfnUnknownException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("Unknown error");
    }

    @Test
    public void cfnUnknownException_noCauseGiven() {
        assertThatExceptionOfType(CfnUnknownException.class).isThrownBy(() -> {
            throw new CfnUnknownException();
        }).withNoCause().withMessageContaining("Unknown error");
    }

    @Test
    public void cfnUnknownException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnUnknownException.class).isThrownBy(() -> {
            throw new CfnUnknownException(new RuntimeException());
        }).satisfies(exception -> Assertions.assertEquals(HandlerErrorCode.Unknown, exception.getErrorCode()));
    }
}
