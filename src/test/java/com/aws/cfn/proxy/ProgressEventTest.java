package com.aws.cfn.proxy;

import com.aws.cfn.TestContext;
import com.aws.cfn.TestModel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgressEventTest {

    @Test
    public void testDefaultFailedHandler() {
        final ProgressEvent<TestModel, TestContext> progressEvent =
            new ProgressEvent<TestModel, TestContext>().defaultFailureHandler(
                new RuntimeException("test error"),
                HandlerErrorCode.InternalFailure);

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelayMinutes()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(progressEvent.getMessage()).isEqualTo("test error");
        assertThat(progressEvent.getResourceModel()).isNull();
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void testDefaultInProgressHandler() {
        final TestModel model = TestModel.builder()
            .property1("abc")
            .property2(123)
            .build();
        final TestContext callbackContet = TestContext.builder()
            .contextPropertyA("def")
            .build();
        final ProgressEvent<TestModel, TestContext> progressEvent =
            new ProgressEvent<TestModel, TestContext>().defaultInProgressHandler(
                callbackContet,
                3,
                model);

        assertThat(progressEvent.getCallbackContext()).isEqualTo(callbackContet);
        assertThat(progressEvent.getCallbackDelayMinutes()).isEqualTo(3);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getResourceModel()).isEqualTo(model);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    }

    @Test
    public void testDefaultSuccessHandler() {
        final TestModel model = TestModel.builder()
            .property1("abc")
            .property2(123)
            .build();
        final ProgressEvent<TestModel, TestContext> progressEvent =
            new ProgressEvent<TestModel, TestContext>().defaultSuccessHandler(
                model);

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelayMinutes()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getResourceModel()).isEqualTo(model);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }
}
