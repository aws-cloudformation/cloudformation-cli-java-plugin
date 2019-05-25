package com.aws.cfn.proxy;

import com.aws.cfn.TestContext;
import com.aws.cfn.TestModel;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ProgressEventTest {

    @Test
    public void testDefaultFailedHandler() {
        final ProgressEvent<TestModel, TestContext> progressEvent =
            new ProgressEvent<TestModel, TestContext>().defaultFailureHandler(
                new RuntimeException("test error"),
                HandlerErrorCode.InternalFailure);

        assertThat(progressEvent.getCallbackContext(), is(nullValue()));
        assertThat(progressEvent.getCallbackDelayMinutes(), is(equalTo(0)));
        assertThat(progressEvent.getErrorCode(), is(equalTo(HandlerErrorCode.InternalFailure)));
        assertThat(progressEvent.getMessage(), is(equalTo("test error")));
        assertThat(progressEvent.getResourceModel(), is(nullValue()));
        assertThat(progressEvent.getResourceModels(), is(nullValue()));
        assertThat(progressEvent.getStatus(), is(equalTo(OperationStatus.FAILED)));
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

        assertThat(progressEvent.getCallbackContext(), is(equalTo(callbackContet)));
        assertThat(progressEvent.getCallbackDelayMinutes(), is(equalTo(3)));
        assertThat(progressEvent.getErrorCode(), is(nullValue()));
        assertThat(progressEvent.getMessage(), is(nullValue()));
        assertThat(progressEvent.getResourceModel(), is(equalTo(model)));
        assertThat(progressEvent.getResourceModels(), is(nullValue()));
        assertThat(progressEvent.getStatus(), is(equalTo(OperationStatus.IN_PROGRESS)));
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

        assertThat(progressEvent.getCallbackContext(), is(nullValue()));
        assertThat(progressEvent.getCallbackDelayMinutes(), is(equalTo(0)));
        assertThat(progressEvent.getErrorCode(), is(nullValue()));
        assertThat(progressEvent.getMessage(), is(nullValue()));
        assertThat(progressEvent.getResourceModel(), is(equalTo(model)));
        assertThat(progressEvent.getResourceModels(), is(nullValue()));
        assertThat(progressEvent.getStatus(), is(equalTo(OperationStatus.SUCCESS)));
    }
}
