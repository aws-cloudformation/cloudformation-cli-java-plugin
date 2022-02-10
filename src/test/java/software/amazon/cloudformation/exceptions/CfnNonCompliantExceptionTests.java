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

public class CfnNonCompliantExceptionTests {
    private static final String TYPE = "AWS::Type::Hook";
    private static final String REASON = "target is not compliant";
    private static final String STATUS = "Non-Complaint";
    private static final String ERROR_MESSAGE = "something wrong";

    @Test
    public void cfnNonCompliantException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnNonCompliantException(TYPE, REASON, new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining(TYPE).withMessageContaining(REASON)
            .withMessageContaining(STATUS);
    }

    @Test
    public void cfnNonCompliantException_singleArgConstructorHasNoMessage() {
        assertThatExceptionOfType(CfnNonCompliantException.class).isThrownBy(() -> {
            throw new CfnNonCompliantException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessage(null);
    }

    @Test
    public void cfnNonCompliantException_noCauseGiven() {
        assertThatExceptionOfType(CfnNonCompliantException.class).isThrownBy(() -> {
            throw new CfnNonCompliantException(TYPE, REASON);
        }).withNoCause().withMessageContaining(TYPE).withMessageContaining(REASON).withMessageContaining(STATUS);
    }

    @Test
    public void cfnNonCompliantException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnNonCompliantException.class).isThrownBy(() -> {
            throw new CfnNonCompliantException(new RuntimeException());
        }).satisfies(exception -> Assertions.assertEquals(HandlerErrorCode.NonCompliant, exception.getErrorCode()));
    }

    @Test
    public void cfnNonCompliantException_errorMessage() {
        assertThatExceptionOfType(CfnNonCompliantException.class).isThrownBy(() -> {
            throw new CfnNonCompliantException(new RuntimeException(ERROR_MESSAGE));
        }).satisfies(exception -> Assertions.assertEquals(ERROR_MESSAGE, exception.getMessage()));
    }
}
