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
package com.amazonaws.cloudformation.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.cloudformation.TestContext;
import com.amazonaws.cloudformation.TestModel;

import org.junit.jupiter.api.Test;

public class ProgressEventTest {

    @Test
    public void testDefaultFailedHandler() {
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent
            .defaultFailureHandler(new RuntimeException("test error"), HandlerErrorCode.InternalFailure);

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(progressEvent.getMessage()).isEqualTo("test error");
        assertThat(progressEvent.getResourceModel()).isNull();
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void testDefaultInProgressHandler() {
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();
        final TestContext callbackContet = TestContext.builder().contextPropertyA("def").build();
        final ProgressEvent<TestModel,
            TestContext> progressEvent = ProgressEvent.defaultInProgressHandler(callbackContet, 3, model);

        assertThat(progressEvent.getCallbackContext()).isEqualTo(callbackContet);
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(3);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getResourceModel()).isEqualTo(model);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    }

    @Test
    public void testDefaultSuccessHandler() {
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent.defaultSuccessHandler(model);

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getResourceModel()).isEqualTo(model);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }
}
