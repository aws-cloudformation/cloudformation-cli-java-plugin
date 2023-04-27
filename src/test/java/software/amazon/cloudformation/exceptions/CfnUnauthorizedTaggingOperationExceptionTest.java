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
import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class CfnUnauthorizedTaggingOperationExceptionTest {
    @Test
    public void cfnUnauthorizedTaggingOperation_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnUnauthorizedTaggingOperationException(new RuntimeException());
        });
    }

    @Test
    public void cfnUnauthorizedTaggingOperationException_singleArgConstructorHasMessage() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnUnauthorizedTaggingOperationException(new RuntimeException());
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("Unauthorized tagging operation");
    }

    @Test
    public void cfnUnauthorizedTaggingOperationException_noCauseGiven() {
        assertThatExceptionOfType(CfnUnauthorizedTaggingOperationException.class).isThrownBy(() -> {
            throw new CfnUnauthorizedTaggingOperationException();
        }).withNoCause().withMessageContaining("Unauthorized tagging operation");
    }

    @Test
    public void cfnUnauthorizedTaggingOperationException_errorCodeIsAppropriate() {
        assertThatExceptionOfType(CfnUnauthorizedTaggingOperationException.class).isThrownBy(() -> {
            throw new CfnUnauthorizedTaggingOperationException(new RuntimeException());
        }).satisfies(exception -> assertEquals(HandlerErrorCode.UnauthorizedTaggingOperation, exception.getErrorCode()));
    }
}
