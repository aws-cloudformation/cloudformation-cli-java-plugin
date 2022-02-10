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
package software.amazon.cloudformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.cloudformation.encryption.KMSCipher;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.HookProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookStatus;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;

@ExtendWith(MockitoExtension.class)
public class HookLambdaWrapperTest {

    private static final String TEST_DATA_BASE_PATH = "src/test/java/software/amazon/cloudformation/data/hook/%s";

    @Mock
    private CredentialsProvider providerLoggingCredentialsProvider;

    @Mock
    private MetricsPublisher platformMetricsPublisher;

    @Mock
    private MetricsPublisher providerMetricsPublisher;

    @Mock
    private CloudWatchLogPublisher providerEventsLogger;

    @Mock
    private LogPublisher platformEventsLogger;

    @Mock
    private SchemaValidator validator;

    @Mock
    private LambdaLogger lambdaLogger;

    @Mock
    private HookHandlerRequest hookHandlerRequest;

    @Mock
    private SdkHttpClient httpClient;

    @Mock
    private KMSCipher cipher;

    private HookLambdaWrapperOverride wrapper;

    @BeforeEach
    public void initWrapper() {
        wrapper = new HookLambdaWrapperOverride(providerLoggingCredentialsProvider, platformEventsLogger, providerEventsLogger,
                                                platformMetricsPublisher, providerMetricsPublisher, validator, httpClient,
                                                cipher);
    }

    private static InputStream loadRequestStream(final String fileName) {
        final File file = new File(String.format(TEST_DATA_BASE_PATH, fileName));

        try {
            return new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Context getLambdaContext() {
        final Context context = mock(Context.class);
        lenient().when(context.getInvokedFunctionArn())
            .thenReturn("arn:aws:lambda:aws-region:acct-id:function:testHookHandler:PROD");
        lenient().when(context.getLogger()).thenReturn(lambdaLogger);
        return context;
    }

    private void verifyInitialiseRuntime() {
        verify(providerLoggingCredentialsProvider).setCredentials(any(Credentials.class));
        verify(platformMetricsPublisher).refreshClient();
        verify(providerMetricsPublisher).refreshClient();
    }

    private void verifyHandlerResponse(final OutputStream out, final HookProgressEvent<TestContext> expected) throws IOException {
        final Serializer serializer = new Serializer();
        final HookProgressEvent<TestContext> handlerResponse = serializer.deserialize(out.toString(),
            new TypeReference<HookProgressEvent<TestContext>>() {
            });

        assertThat(handlerResponse.getClientRequestToken()).isEqualTo(expected.getClientRequestToken());
        assertThat(handlerResponse.getHookStatus()).isEqualTo(expected.getHookStatus());
        assertThat(handlerResponse.getErrorCode()).isEqualTo(expected.getErrorCode());
        assertThat(handlerResponse.getMessage()).isEqualTo(expected.getMessage());
        assertThat(handlerResponse.getResult()).isEqualTo(expected.getResult());
        assertThat(handlerResponse.getCallbackContext()).isEqualTo(expected.getCallbackContext());
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(expected.getCallbackDelaySeconds());
    }

    @ParameterizedTest
    @CsvSource({ "preCreate.request.json,CREATE_PRE_PROVISION", "preUpdate.request.json,UPDATE_PRE_PROVISION",
        "preDelete.request.json,DELETE_PRE_PROVISION" })
    public void invokeHandler_CompleteSynchronously_returnsSuccess(final String requestDataPath,
                                                                   final String invocationPointString)
        throws IOException {
        final HookInvocationPoint invocationPoint = HookInvocationPoint.valueOf(invocationPointString);

        // if the handler responds Complete, this is treated as a successful synchronous
        // completion
        final ProgressEvent<TestModel,
            TestContext> pe = ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.SUCCESS).build();
        wrapper.setInvokeHandlerResponse(pe);

        lenient().when(cipher.decryptCredentials(any())).thenReturn(new Credentials("123", "123", "123"));

        wrapper.setTransformResponse(hookHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // all metrics should be published, once for a single invocation
            verify(platformMetricsPublisher, times(1)).publishInvocationMetric(any(Instant.class), eq(invocationPoint));
            verify(platformMetricsPublisher, times(1)).publishDurationMetric(any(Instant.class), eq(invocationPoint), anyLong());
            verify(platformMetricsPublisher, times(1)).publishExceptionByErrorCodeAndCountBulkMetrics(any(Instant.class),
                any(HookInvocationPoint.class), isNull());

            // validation failure metric should not be published
            verifyNoMoreInteractions(platformMetricsPublisher);

            // verify output response
            verifyHandlerResponse(out,
                HookProgressEvent.<TestContext>builder().clientRequestToken("123456").hookStatus(HookStatus.SUCCESS).build());

            // assert handler receives correct injections
            assertThat(wrapper.awsClientProxy).isNotNull();
            assertThat(wrapper.getRequest()).isEqualTo(hookHandlerRequest);
            assertThat(wrapper.invocationPoint).isEqualTo(invocationPoint);
            assertThat(wrapper.callbackContext).isNull();
        }
    }
}
