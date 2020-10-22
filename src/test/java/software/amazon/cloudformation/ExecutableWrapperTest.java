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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;

@ExtendWith(MockitoExtension.class)
public class ExecutableWrapperTest {
    private static final String TEST_DATA_BASE_PATH = "src/test/java/software/amazon/cloudformation/data/%s";

    @Mock
    private CredentialsProvider providerLoggingCredentialsProvider;

    @Mock
    private MetricsPublisher providerMetricsPublisher;

    @Mock
    private CloudWatchLogPublisher providerEventsLogger;

    @Mock
    private LogPublisher platformEventsLogger;

    @Mock
    private SchemaValidator validator;

    @Mock
    private ResourceHandlerRequest<TestModel> resourceHandlerRequest;

    @Mock
    private SdkHttpClient httpClient;

    private ExecutableWrapperOverride wrapper;

    @BeforeEach
    public void initWrapper() {
        wrapper = new ExecutableWrapperOverride(providerLoggingCredentialsProvider, platformEventsLogger, providerEventsLogger,
                                                providerMetricsPublisher, validator, httpClient);
    }

    public static InputStream loadRequestStream(final String fileName) {
        final File file = new File(String.format(TEST_DATA_BASE_PATH, fileName));

        try {
            return new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void verifyInitialiseRuntime() {
        verify(providerLoggingCredentialsProvider).setCredentials(any(Credentials.class));
        verify(providerMetricsPublisher).refreshClient();
    }

    private void verifyHandlerResponse(final OutputStream out, final ProgressEvent<TestModel, TestContext> expected)
        throws IOException {
        final Serializer serializer = new Serializer();
        final ProgressEvent<TestModel, TestContext> handlerResponse = serializer.deserialize(out.toString(),
            new TypeReference<ProgressEvent<TestModel, TestContext>>() {
            });

        assertThat(handlerResponse.getErrorCode()).isEqualTo(expected.getErrorCode());
        assertThat(handlerResponse.getNextToken()).isEqualTo(expected.getNextToken());
        assertThat(handlerResponse.getStatus()).isEqualTo(expected.getStatus());
        assertThat(handlerResponse.getResourceModel()).isEqualTo(expected.getResourceModel());
    }

    @ParameterizedTest
    @CsvSource({ "create.request.json,CREATE", "update.request.json,UPDATE", "delete.request.json,DELETE",
        "read.request.json,READ", "list.request.json,LIST" })
    public void invokeHandler_CompleteSynchronously_returnsSuccess(final String requestDataPath, final String actionAsString)
        throws IOException {
        final Action action = Action.valueOf(actionAsString);
        final TestModel model = new TestModel();

        // if the handler responds Complete, this is treated as a successful synchronous
        // completion
        final ProgressEvent<TestModel,
            TestContext> pe = ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.SUCCESS).build();
        wrapper.setInvokeHandlerResponse(pe);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            wrapper.handleRequest(in, out);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));
            }

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.SUCCESS).build());

            // assert handler receives correct injections
            assertThat(wrapper.awsClientProxy).isNotNull();
            assertThat(wrapper.getRequest()).isEqualTo(resourceHandlerRequest);
            assertThat(wrapper.action).isEqualTo(action);
            assertThat(wrapper.callbackContext).isNull();
        }
    }
}
