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
package software.amazon.cloudformation.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.TestContext;
import software.amazon.cloudformation.proxy.hook.HookAnnotation;
import software.amazon.cloudformation.proxy.hook.HookAnnotationSeverityLevel;
import software.amazon.cloudformation.proxy.hook.HookAnnotationStatus;
import software.amazon.cloudformation.proxy.hook.HookProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookStatus;
import software.amazon.cloudformation.resource.Serializer;

public class HookProgressEventTest {

    @Test
    public void testFailedHandler() {
        final HookProgressEvent<
            TestContext> progressEvent = HookProgressEvent.failed(null, HandlerErrorCode.InternalFailure, "test error", null);

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(progressEvent.getMessage()).isEqualTo("test error");
        assertThat(progressEvent.getHookStatus()).isEqualTo(HookStatus.FAILED);
        assertThat(progressEvent.isFailed()).isTrue();
        assertThat(progressEvent.isComplete()).isFalse();
        assertThat(progressEvent.isInProgress()).isFalse();
        assertThat(progressEvent.canContinueProgress()).isFalse();
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void testInProgressHandler() {
        final TestContext callbackContext = TestContext.builder().contextPropertyA("def").build();
        final HookProgressEvent<TestContext> progressEvent = HookProgressEvent.progress(callbackContext, null, null, 3);

        assertThat(progressEvent.getCallbackContext()).isEqualTo(callbackContext);
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(3);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getHookStatus()).isEqualTo(HookStatus.IN_PROGRESS);
        assertThat(progressEvent.isFailed()).isFalse();
        assertThat(progressEvent.isComplete()).isFalse();
        assertThat(progressEvent.isInProgress()).isTrue();
        assertThat(progressEvent.canContinueProgress()).isFalse();
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void testInProgressHandler_isContinuable() {
        final TestContext callbackContext = TestContext.builder().contextPropertyA("def").build();
        final HookProgressEvent<TestContext> progressEvent = HookProgressEvent.progress(callbackContext, null, null, 0);

        assertThat(progressEvent.getCallbackContext()).isEqualTo(callbackContext);
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getHookStatus()).isEqualTo(HookStatus.IN_PROGRESS);
        assertThat(progressEvent.isFailed()).isFalse();
        assertThat(progressEvent.isComplete()).isFalse();
        assertThat(progressEvent.isInProgress()).isTrue();
        assertThat(progressEvent.canContinueProgress()).isTrue();
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void testCompleteHandler() {
        final HookProgressEvent<TestContext> progressEvent = HookProgressEvent.complete(null, null, null);

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getHookStatus()).isEqualTo(HookStatus.SUCCESS);
        assertThat(progressEvent.isFailed()).isFalse();
        assertThat(progressEvent.isComplete()).isTrue();
        assertThat(progressEvent.isInProgress()).isFalse();
        assertThat(progressEvent.canContinueProgress()).isFalse();
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void testDefaultFailedHandler() {
        final HookProgressEvent<TestContext> progressEvent = HookProgressEvent.defaultFailureHandler(new Exception("test error"),
            HandlerErrorCode.InternalFailure);

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(progressEvent.getMessage()).isEqualTo("test error");
        assertThat(progressEvent.getHookStatus()).isEqualTo(HookStatus.FAILED);
        assertThat(progressEvent.isFailed()).isTrue();
        assertThat(progressEvent.isComplete()).isFalse();
        assertThat(progressEvent.isInProgress()).isFalse();
        assertThat(progressEvent.canContinueProgress()).isFalse();
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void testDefaultInProgressHandler() {
        final TestContext callbackContext = TestContext.builder().contextPropertyA("def").build();
        final HookProgressEvent<TestContext> progressEvent = HookProgressEvent.defaultProgressHandler(callbackContext, 3);

        assertThat(progressEvent.getCallbackContext()).isEqualTo(callbackContext);
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(3);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getHookStatus()).isEqualTo(HookStatus.IN_PROGRESS);
        assertThat(progressEvent.isFailed()).isFalse();
        assertThat(progressEvent.isComplete()).isFalse();
        assertThat(progressEvent.isInProgress()).isTrue();
        assertThat(progressEvent.canContinueProgress()).isFalse();
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void testDefaultCompleteHandler() {
        final HookProgressEvent<TestContext> progressEvent = HookProgressEvent.defaultCompleteHandler();

        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getErrorCode()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getHookStatus()).isEqualTo(HookStatus.SUCCESS);
        assertThat(progressEvent.isFailed()).isFalse();
        assertThat(progressEvent.isComplete()).isTrue();
        assertThat(progressEvent.isInProgress()).isFalse();
        assertThat(progressEvent.canContinueProgress()).isFalse();
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void testOnCompleteChaining() {
        final HookProgressEvent<TestContext> progressEvent = HookProgressEvent.complete(null, null, null);
        final HookProgressEvent<TestContext> chained = progressEvent
            .onComplete(e -> HookProgressEvent.failed(null, HandlerErrorCode.InternalFailure, "test error", null));

        assertThat(chained.getCallbackContext()).isNull();
        assertThat(chained.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(chained.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(chained.getHookStatus()).isEqualTo(HookStatus.FAILED);
        assertThat(chained.getMessage()).isEqualTo("test error");
        assertThat(chained.isFailed()).isEqualTo(true);
        assertThat(chained.isInProgress()).isEqualTo(false);
        assertThat(chained.isInProgressCallbackDelay()).isEqualTo(false);
        assertThat(progressEvent.getAnnotations()).isNull();
    }

    @Test
    public void progressEvent_serialize_shouldReturnJson() throws JsonProcessingException {
        final HookProgressEvent<String> progressEvent = HookProgressEvent.complete(null, null, null);
        final Serializer serializer = new Serializer();
        final String json = serializer.serialize(progressEvent);

        assertThat(json).isEqualTo("{\"hookStatus\":\"SUCCESS\",\"callbackDelaySeconds\":0}");
    }

    @Test
    public void progressEvent_with_annotations_serialize_shouldReturnJson() throws JsonProcessingException {
        final List<HookAnnotation> annotations = List.of(
            HookAnnotation.builder().annotationName("test1").status(HookAnnotationStatus.PASSED).build(),
            HookAnnotation.builder().annotationName("test2").status(HookAnnotationStatus.FAILED).statusMessage("test-message-2")
                .remediationMessage("test-remediation-message-2").remediationLink("https://localhost")
                .severityLevel(HookAnnotationSeverityLevel.CRITICAL).build());

        final HookProgressEvent<
            Object> progressEvent = HookProgressEvent.builder().hookStatus(HookStatus.SUCCESS).annotations(annotations).build();
        final Serializer serializer = new Serializer();
        final String json = serializer.serialize(progressEvent);

        assertThat(json).isEqualTo(
            "{\"hookStatus\":\"SUCCESS\",\"callbackDelaySeconds\":0,\"annotations\":[{\"annotationName\":\"test1\",\"status\":\"PASSED\"},{\"annotationName\":\"test2\",\"status\":\"FAILED\",\"statusMessage\":\"test-message-2\",\"remediationMessage\":\"test-remediation-message-2\",\"remediationLink\":\"https://localhost\",\"severityLevel\":\"CRITICAL\"}]}");
    }
}
