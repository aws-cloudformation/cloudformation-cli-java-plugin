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

public class CfnNotStabilizedExceptionTests {
    @Test
    public void cfnNotStabilizedException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnNotStabilizedException("AWS::Type::Resource", "myId", new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("AWS::Type::Resource").withMessageContaining("myId")
            .withMessageContaining("not stabilize");
    }

    @Test
    public void cfnNotStabilizedException_singleArgConstructorHasNoMessage() {
        assertThatExceptionOfType(CfnNotStabilizedException.class).isThrownBy(() -> {
            throw new CfnNotStabilizedException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessage(null);
    }

    @Test
    public void cfnNotStabilizedException_noCauseGiven() {
        assertThatExceptionOfType(CfnNotStabilizedException.class).isThrownBy(() -> {
            throw new CfnNotStabilizedException("AWS::Type::Resource", "myId");
        }).withNoCause().withMessageContaining("AWS::Type::Resource").withMessageContaining("myId")
            .withMessageContaining("not stabilize");
    }

    @Test
    public void cfnNotStabilizedException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnNotStabilizedException.class).isThrownBy(() -> {
            throw new CfnNotStabilizedException(new RuntimeException());
        }).satisfies(exception -> assertEquals(HandlerErrorCode.NotStabilized, exception.getErrorCode()));
    }

    @Test
    public void cfnNotStabilizedException_errorMessage() {
        assertThatExceptionOfType(CfnNotStabilizedException.class).isThrownBy(() -> {
            throw new CfnNotStabilizedException(new RuntimeException("something wrong"));
        }).satisfies(exception -> assertEquals("something wrong", exception.getMessage()));
    }
}
