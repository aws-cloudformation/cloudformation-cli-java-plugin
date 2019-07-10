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
import com.amazonaws.cloudformation.exceptions.ResourceNotFoundException;
import com.amazonaws.cloudformation.resource.Serializer;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.json.JSONObject;
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

    @Test
    public void testOnSuccessChain() {
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent.defaultSuccessHandler(model);
        final ProgressEvent<TestModel, TestContext> chained = progressEvent
                .onSuccess(e -> ProgressEvent.failed(e.getResourceModel(), null, HandlerErrorCode.ServiceLimitExceeded, "Exceeded"));

        assertThat(chained.getCallbackContext()).isNull();
        assertThat(chained.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(chained.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);
        assertThat(chained.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(chained.getMessage()).isEqualTo("Exceeded");
        assertThat(chained.isFailed()).isEqualTo(true);
        assertThat(chained.isInProgress()).isEqualTo(false);
        assertThat(chained.isInProgressCallbackDelay()).isEqualTo(false);
    }

    @Test
    public void testOnSuccessMethod_Success() {
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent.defaultSuccessHandler(model);

        progressEvent.onSuccess((ProgressEvent<TestModel, TestContext> testModelTestContextProgressEvent) -> progressEvent);
        progressEvent.isSuccess();
        progressEvent.isInProgressCallbackDelay();
    }

    @Test
    public void testOnSuccessMethod_NoStatus() {
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent.defaultSuccessHandler(model);
        progressEvent.setStatus(null);

        progressEvent.onSuccess((ProgressEvent<TestModel, TestContext> testModelTestContextProgressEvent) -> progressEvent);
        progressEvent.isSuccess();
        progressEvent.isInProgressCallbackDelay();
    }

    @Test
    public void testOnSuccessMethod_InProgress1() {
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent.defaultInProgressHandler(null, 30, model);

        progressEvent.onSuccess((ProgressEvent<TestModel, TestContext> testModelTestContextProgressEvent) -> progressEvent);
        progressEvent.isSuccess();
        progressEvent.isInProgressCallbackDelay();
    }

    @Test
    public void testOnSuccessMethod_InProgress2() {
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent.defaultInProgressHandler(null, 0, model);

        progressEvent.onSuccess((ProgressEvent<TestModel, TestContext> testModelTestContextProgressEvent) -> progressEvent);
        progressEvent.isSuccess();
        progressEvent.isInProgressCallbackDelay();
    }

    @Test
    public void testOnSuccessMethod_Failed() {
        final ProgressEvent<TestModel, TestContext> progressEvent = ProgressEvent.defaultFailureHandler(
            new ResourceNotFoundException(new RuntimeException("Sorry")), HandlerErrorCode.InternalFailure);

        progressEvent.onSuccess((ProgressEvent<TestModel, TestContext> testModelTestContextProgressEvent) -> progressEvent);
    }

    @Test
    public void progressEvent_serialize_shouldReturnJson() throws JsonProcessingException {
        final ProgressEvent<String, String> progressEvent = ProgressEvent.defaultSuccessHandler("");
        final Serializer serializer = new Serializer();
        final JSONObject json = serializer.serialize(progressEvent);

        // careful if you add new properties here. downstream has to be able to handle
        // them
        assertThat(json).hasToString("{\"callbackDelaySeconds\":0,\"resourceModel\":\"\",\"status\":\"SUCCESS\"}");
    }
}
