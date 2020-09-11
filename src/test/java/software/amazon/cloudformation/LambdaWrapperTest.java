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
import static org.mockito.Mockito.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CloudWatchLogsException;
import software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.RequestData;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;
import software.amazon.cloudformation.resource.Validator;
import software.amazon.cloudformation.resource.exceptions.ValidationException;

@ExtendWith(MockitoExtension.class)
public class LambdaWrapperTest {

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
    private LambdaLogger lambdaLogger;

    @Mock
    private ResourceHandlerRequest<TestModel> resourceHandlerRequest;

    @Mock
    private SdkHttpClient httpClient;

    private WrapperOverride wrapper;

    @BeforeEach
    public void initWrapper() {
        wrapper = new WrapperOverride(providerLoggingCredentialsProvider, platformEventsLogger, providerEventsLogger,
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

    private Context getLambdaContext() {
        final Context context = mock(Context.class);
        lenient().when(context.getInvokedFunctionArn()).thenReturn("arn:aws:lambda:aws-region:acct-id:function:testHandler:PROD");
        lenient().when(context.getLogger()).thenReturn(lambdaLogger);
        return context;
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
    public void invokeHandler_nullResponse_returnsFailure(final String requestDataPath, final String actionAsString)
        throws IOException {
        final Action action = Action.valueOf(actionAsString);
        final TestModel model = new TestModel();

        // a null response is a terminal fault
        wrapper.setInvokeHandlerResponse(null);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // validation failure metric should be published for final error handling
            verify(providerMetricsPublisher).publishExceptionMetric(any(Instant.class), any(), any(TerminalException.class),
                any(HandlerErrorCode.class));
            verify(providerMetricsPublisher).publishExceptionByErrorCodeMetric(any(Instant.class), any(),
                any(HandlerErrorCode.class), eq(Boolean.TRUE));
            verify(providerMetricsPublisher).publishExceptionCountMetric(any(Instant.class), any(), any(Boolean.class));

            // all metrics should be published even on terminal failure
            verify(providerMetricsPublisher).publishInvocationMetric(any(Instant.class), eq(action));
            verify(providerMetricsPublisher).publishDurationMetric(any(Instant.class), eq(action), anyLong());

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));
            }

            verify(providerEventsLogger).refreshClient();
            verify(providerEventsLogger, times(2)).publishLogEvent(any());
            verifyNoMoreInteractions(providerEventsLogger);

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                .status(OperationStatus.FAILED).message("Handler failed to provide a response.").build());

            // assert handler receives correct injections
            assertThat(wrapper.awsClientProxy).isNotNull();
            assertThat(wrapper.getRequest()).isEqualTo(resourceHandlerRequest);
            assertThat(wrapper.action).isEqualTo(action);
            assertThat(wrapper.callbackContext).isNull();
        }
    }

    @Test
    public void invokeHandlerForCreate_without_customer_loggingCredentials() throws IOException {
        invokeHandler_without_customerLoggingCredentials("create.request-without-logging-credentials.json", Action.CREATE);
    }

    private void invokeHandler_without_customerLoggingCredentials(final String requestDataPath, final Action action)
        throws IOException {
        final TestModel model = new TestModel();

        // a null response is a terminal fault
        wrapper.setInvokeHandlerResponse(null);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verify(providerLoggingCredentialsProvider, times(0)).setCredentials(any(Credentials.class));
            verify(providerMetricsPublisher, times(0)).refreshClient();

            // validation failure metric should be published for final error handling
            verify(providerMetricsPublisher, times(0)).publishExceptionMetric(any(Instant.class), any(),
                any(TerminalException.class), any(HandlerErrorCode.class));

            // all metrics should be published even on terminal failure
            verify(providerMetricsPublisher, times(0)).publishInvocationMetric(any(Instant.class), eq(action));
            verify(providerMetricsPublisher, times(0)).publishDurationMetric(any(Instant.class), eq(action), anyLong());

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));
            }

            verifyNoMoreInteractions(providerEventsLogger);

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                .status(OperationStatus.FAILED).message("Handler failed to provide a response.").build());
        }
    }

    @ParameterizedTest
    @CsvSource({ "create.request.json,CREATE", "update.request.json,UPDATE", "delete.request.json,DELETE",
        "read.request.json,READ", "list.request.json,LIST" })
    public void invokeHandler_handlerFailed_returnsFailure(final String requestDataPath, final String actionAsString)
        throws IOException {
        final Action action = Action.valueOf(actionAsString);

        // explicit fault response is treated as an unsuccessful synchronous completion
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.FAILED).errorCode(HandlerErrorCode.InternalFailure).message("Custom Fault").build();
        wrapper.setInvokeHandlerResponse(pe);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));
            }

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                .status(OperationStatus.FAILED).message("Custom Fault").build());

            // assert handler receives correct injections
            assertThat(wrapper.awsClientProxy).isNotNull();
            assertThat(wrapper.getRequest()).isEqualTo(resourceHandlerRequest);
            assertThat(wrapper.action).isEqualTo(action);
            assertThat(wrapper.callbackContext).isNull();
        }
    }

    @Test
    public void invokeHandler_withNullInput() throws IOException {
        // explicit fault response is treated as an unsuccessful synchronous completion
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.FAILED).errorCode(HandlerErrorCode.InternalFailure).message("Custom Fault").build();
        wrapper.setInvokeHandlerResponse(pe);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = null; final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();
            wrapper.handleRequest(in, out, context);
            verifyNoMoreInteractions(providerMetricsPublisher, providerEventsLogger);
        }
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
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

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

    @Test
    public void invokeHandler_DependenciesInitialised_CompleteSynchronously_returnsSuccess() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride();
        final TestModel model = new TestModel();

        // if the handler responds Complete, this is treated as a successful synchronous
        // completion
        final ProgressEvent<TestModel,
            TestContext> pe = ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.SUCCESS).build();
        wrapper.setInvokeHandlerResponse(pe);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);

        wrapper.setTransformResponse(resourceHandlerRequest);

        // use a request context in our payload to bypass certain callbacks
        try (final InputStream in = loadRequestStream("create.with-callback-context.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // simply ensure all dependencies were setup correctly - behaviour is tested
            // through mocks
            assertThat(wrapper.serializer).isNotNull();
            assertThat(wrapper.loggerProxy).isNotNull();
            assertThat(wrapper.metricsPublisherProxy).isNotNull();
            assertThat(wrapper.lambdaLogger).isNotNull();
            assertThat(wrapper.providerCredentialsProvider).isNotNull();
            assertThat(wrapper.providerCloudWatchProvider).isNotNull();
            assertThat(wrapper.cloudWatchLogsProvider).isNotNull();
            assertThat(wrapper.validator).isNotNull();
        }
    }

    @ParameterizedTest
    @CsvSource({ "create.request.json,CREATE", "update.request.json,UPDATE", "delete.request.json,DELETE",
        "read.request.json,READ", "list.request.json,LIST" })
    public void invokeHandler_InProgress_returnsInProgress(final String requestDataPath, final String actionAsString)
        throws IOException {
        final Action action = Action.valueOf(actionAsString);
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.IN_PROGRESS).resourceModel(model).build();
        wrapper.setInvokeHandlerResponse(pe);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // all metrics should be published, once for a single invocation
            verify(providerMetricsPublisher).publishInvocationMetric(any(Instant.class), eq(action));
            verify(providerMetricsPublisher).publishDurationMetric(any(Instant.class), eq(action), anyLong());

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator).validateObject(any(JSONObject.class), any(JSONObject.class));

                // verify output response
                verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.IN_PROGRESS)
                    .resourceModel(TestModel.builder().property1("abc").property2(123).build()).build());
            } else {
                verifyHandlerResponse(out,
                    ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.InternalFailure).message("READ and LIST handlers must return synchronously.")
                        .build());
                verify(providerMetricsPublisher).publishExceptionMetric(any(Instant.class), eq(action),
                    any(TerminalException.class), eq(HandlerErrorCode.InternalFailure));

            }

            verify(providerMetricsPublisher, atLeastOnce()).publishExceptionByErrorCodeMetric(any(Instant.class), eq(action),
                any(), any(Boolean.class));
            verify(providerMetricsPublisher).publishExceptionCountMetric(any(Instant.class), any(), any(Boolean.class));

            // validation failure metric should not be published
            verifyNoMoreInteractions(providerMetricsPublisher);

            // assert handler receives correct injections
            assertThat(wrapper.awsClientProxy).isNotNull();
            assertThat(wrapper.getRequest()).isEqualTo(resourceHandlerRequest);
            assertThat(wrapper.action).isEqualTo(action);
            assertThat(wrapper.callbackContext).isNull();
        }
    }

    @ParameterizedTest
    @CsvSource({ "create.with-callback-context.request.json,CREATE", "update.with-callback-context.request.json,UPDATE",
        "delete.with-callback-context.request.json,DELETE" })
    public void reInvokeHandler_InProgress_returnsInProgress(final String requestDataPath, final String actionAsString)
        throws IOException {
        final Action action = Action.valueOf(actionAsString);
        final TestModel model = TestModel.builder().property1("abc").property2(123).build();

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used, and any
        // interval >= 1 minute is scheduled
        // against CloudWatch. Shorter intervals are able to run locally within same
        // function context if runtime permits
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.IN_PROGRESS).callbackDelaySeconds(60).resourceModel(model).build();
        wrapper.setInvokeHandlerResponse(pe);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // all metrics should be published, once for a single invocation
            verify(providerMetricsPublisher).publishInvocationMetric(any(Instant.class), eq(action));
            verify(providerMetricsPublisher).publishDurationMetric(any(Instant.class), eq(action), anyLong());
            verify(providerMetricsPublisher, atLeastOnce()).publishExceptionByErrorCodeMetric(any(Instant.class), eq(action),
                any(), eq(Boolean.FALSE));
            verify(providerMetricsPublisher).publishExceptionCountMetric(any(Instant.class), eq(action), eq(Boolean.FALSE));

            // validation failure metric should not be published
            verifyNoMoreInteractions(providerMetricsPublisher);

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));
            }

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.IN_PROGRESS)
                .resourceModel(TestModel.builder().property1("abc").property2(123).build()).build());
        }
    }

    @ParameterizedTest
    @CsvSource({ "create.request.json,CREATE", "update.request.json,UPDATE", "delete.request.json,DELETE" })
    public void invokeHandler_SchemaValidationFailure(final String requestDataPath, final String actionAsString)
        throws IOException {
        final Action action = Action.valueOf(actionAsString);
        doThrow(ValidationException.class).when(validator).validateObject(any(JSONObject.class), any(JSONObject.class));

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // validation failure metric should be published but no others
            verify(providerMetricsPublisher, times(1)).publishExceptionMetric(any(Instant.class), eq(action),
                any(Exception.class), any(HandlerErrorCode.class));

            // all metrics should be published, even for a single invocation
            verify(providerMetricsPublisher, times(1)).publishInvocationMetric(any(Instant.class), eq(action));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));
            }

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InvalidRequest)
                .status(OperationStatus.FAILED).message("Model validation failed with unknown cause.").build());
        }
    }

    @Test
    public void invokeHandler_invalidModelTypes_causesSchemaValidationFailure() throws IOException {
        // use actual validator to verify behaviour
        final WrapperOverride wrapper = new WrapperOverride(providerLoggingCredentialsProvider, platformEventsLogger,
                                                            providerEventsLogger, providerMetricsPublisher, new Validator() {
                                                            }, httpClient);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream("create.request.with-invalid-model-types.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            verifyHandlerResponse(out,
                ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InvalidRequest)
                    .status(OperationStatus.FAILED)
                    .message("Model validation failed (#/property1: expected type: String, found: JSONArray)").build());
        }
    }

    @Test
    public void invokeHandler_extraneousModelFields_causesSchemaValidationFailure() throws IOException {
        // use actual validator to verify behaviour
        final WrapperOverride wrapper = new WrapperOverride(providerLoggingCredentialsProvider, platformEventsLogger,
                                                            providerEventsLogger, providerMetricsPublisher, new Validator() {
                                                            }, httpClient);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream("create.request.with-extraneous-model-fields.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // validation failure metric should be published but no others
            verify(providerMetricsPublisher, times(1)).publishExceptionMetric(any(Instant.class), eq(Action.CREATE),
                any(Exception.class), any(HandlerErrorCode.class));

            // all metrics should be published, even for a single invocation
            verify(providerMetricsPublisher, times(1)).publishInvocationMetric(any(Instant.class), eq(Action.CREATE));

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InvalidRequest)
                .status(OperationStatus.FAILED)
                .message("Model validation failed (#: extraneous key [fieldCausesValidationError] is not permitted)").build());
        }
    }

    @Test
    public void invokeHandler_withMalformedRequest_causesSchemaValidationFailure() throws IOException {
        final TestModel model = new TestModel();

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS).resourceModel(model).build();
        wrapper.setInvokeHandlerResponse(pe);

        wrapper.setTransformResponse(resourceHandlerRequest);

        // our ObjectMapper implementation will ignore extraneous fields rather than
        // fail them
        // this slightly loosens the coupling between caller (CloudFormation) and
        // handlers.
        try (final InputStream in = loadRequestStream("malformed.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.SUCCESS)
                .resourceModel(TestModel.builder().build()).build());
        }
    }

    @Test
    public void invokeHandler_withoutCallerCredentials_passesNoAWSProxy() throws IOException {
        final TestModel model = new TestModel();
        model.setProperty1("abc");
        model.setProperty2(123);
        wrapper.setTransformResponse(resourceHandlerRequest);

        // respond with immediate success to avoid callback invocation
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS).resourceModel(model).build();
        wrapper.setInvokeHandlerResponse(pe);

        try (final InputStream in = loadRequestStream("create.request-without-caller-credentials.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.SUCCESS)
                .resourceModel(TestModel.builder().property1("abc").property2(123).build()).build());

            // proxy should be null by virtue of having not had callerCredentials passed in
            assertThat(wrapper.awsClientProxy).isNull();
        }
    }

    @Test
    public void invokeHandler_withDefaultInjection_returnsSuccess() throws IOException {
        final TestModel model = new TestModel();
        model.setProperty1("abc");
        model.setProperty2(123);
        wrapper.setTransformResponse(resourceHandlerRequest);

        // respond with immediate success to avoid callback invocation
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS).resourceModel(model).build();
        wrapper.setInvokeHandlerResponse(pe);

        try (final InputStream in = loadRequestStream("create.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.SUCCESS)
                .resourceModel(TestModel.builder().property1("abc").property2(123).build()).build());

            // proxy uses caller credentials and will be injected
            assertThat(wrapper.awsClientProxy).isNotNull();
        }
    }

    @Test
    public void invokeHandler_withDefaultInjection_returnsInProgress() throws IOException {
        final TestModel model = new TestModel();
        model.setProperty1("abc");
        model.setProperty2(123);
        wrapper.setTransformResponse(resourceHandlerRequest);

        // respond with immediate success to avoid callback invocation
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.IN_PROGRESS).resourceModel(model).build();
        wrapper.setInvokeHandlerResponse(pe);

        try (final InputStream in = loadRequestStream("create.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();
            wrapper.handleRequest(in, out, context);

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().status(OperationStatus.IN_PROGRESS)
                .resourceModel(TestModel.builder().property1("abc").property2(123).build()).build());
        }
    }

    @Test
    public void invokeHandler_clientsRefreshedOnEveryInvoke() throws IOException {
        Context context = getLambdaContext();
        try (InputStream in = loadRequestStream("create.request.json"); OutputStream out = new ByteArrayOutputStream()) {
            wrapper.handleRequest(in, out, context);
        }

        // invoke the same wrapper instance again to ensure client is refreshed
        context = getLambdaContext();
        try (InputStream in = loadRequestStream("create.request.json"); OutputStream out = new ByteArrayOutputStream()) {
            wrapper.handleRequest(in, out, context);
        }

    }

    @Test
    public void invokeHandler_throwsAmazonServiceException_returnsServiceException() throws IOException {
        // exceptions are caught consistently by LambdaWrapper
        wrapper.setInvokeHandlerException(new AmazonServiceException("some error"));

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream("create.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // all metrics should be published, once for a single invocation
            verify(providerMetricsPublisher, times(1)).publishInvocationMetric(any(Instant.class), eq(Action.CREATE));
            verify(providerMetricsPublisher, times(1)).publishDurationMetric(any(Instant.class), eq(Action.CREATE), anyLong());

            // failure metric should be published
            verify(providerMetricsPublisher, times(1)).publishExceptionMetric(any(Instant.class), any(),
                any(AmazonServiceException.class), any(HandlerErrorCode.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));

            // verify output response
            verifyHandlerResponse(out,
                ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.GeneralServiceException)
                    .status(OperationStatus.FAILED)
                    .message("some error (Service: null; Status Code: 0; Error Code: null; Request ID: null)").build());
        }
    }

    @Test
    public void invokeHandler_throwsSDK2ServiceException_returnsServiceException() throws IOException {
        wrapper.setInvokeHandlerException(CloudWatchLogsException.builder().build());

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream("create.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // all metrics should be published, once for a single invocation
            verify(providerMetricsPublisher, times(1)).publishInvocationMetric(any(Instant.class), eq(Action.CREATE));
            verify(providerMetricsPublisher, times(1)).publishDurationMetric(any(Instant.class), eq(Action.CREATE), anyLong());

            // failure metric should be published
            verify(providerMetricsPublisher, times(1)).publishExceptionMetric(any(Instant.class), any(),
                any(CloudWatchLogsException.class), any(HandlerErrorCode.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));

            // verify output response
            verifyHandlerResponse(out,
                ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.GeneralServiceException)
                    .status(OperationStatus.FAILED)
                    .message("some error (Service: null; Status Code: 0; Error Code: null; Request ID: null)").build());
        }
    }

    @Test
    public void invokeHandler_throwsResourceAlreadyExistsException_returnsAlreadyExists() throws IOException {
        // exceptions are caught consistently by LambdaWrapper
        wrapper.setInvokeHandlerException(new ResourceAlreadyExistsException("AWS::Test::TestModel", "id-1234"));

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream("create.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // all metrics should be published, once for a single invocation
            verify(providerMetricsPublisher, times(1)).publishInvocationMetric(any(Instant.class), eq(Action.CREATE));
            verify(providerMetricsPublisher, times(1)).publishDurationMetric(any(Instant.class), eq(Action.CREATE), anyLong());

            // failure metric should be published
            verify(providerMetricsPublisher, times(1)).publishExceptionMetric(any(Instant.class), any(),
                any(ResourceAlreadyExistsException.class), any(HandlerErrorCode.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));

            // verify output response
            verifyHandlerResponse(out,
                ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.AlreadyExists)
                    .status(OperationStatus.FAILED)
                    .message("Resource of type 'AWS::Test::TestModel' with identifier 'id-1234' already exists.").build());
        }
    }

    @Test
    public void invokeHandler_throwsResourceNotFoundException_returnsNotFound() throws IOException {
        // exceptions are caught consistently by LambdaWrapper
        wrapper.setInvokeHandlerException(new ResourceNotFoundException("AWS::Test::TestModel", "id-1234"));

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream("create.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            // all metrics should be published, once for a single invocation
            verify(providerMetricsPublisher, times(1)).publishInvocationMetric(any(Instant.class), eq(Action.CREATE));
            verify(providerMetricsPublisher, times(1)).publishDurationMetric(any(Instant.class), eq(Action.CREATE), anyLong());

            // failure metric should be published
            verify(providerMetricsPublisher, times(1)).publishExceptionMetric(any(Instant.class), any(),
                any(ResourceNotFoundException.class), any(HandlerErrorCode.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            verify(validator, times(1)).validateObject(any(JSONObject.class), any(JSONObject.class));

            // verify output response
            verifyHandlerResponse(out,
                ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.NotFound)
                    .status(OperationStatus.FAILED)
                    .message("Resource of type 'AWS::Test::TestModel' with identifier 'id-1234' was not found.").build());
        }
    }

    @Test
    public void invokeHandler_metricPublisherThrowable_returnsFailureResponse() throws IOException {
        // simulate runtime Errors in the metrics publisher (such as dependency
        // resolution conflicts)
        doThrow(new Error("not an Exception")).when(providerMetricsPublisher).publishInvocationMetric(any(), any());
        doThrow(new Error("not an Exception")).when(providerMetricsPublisher).publishExceptionMetric(any(), any(), any(), any());

        try (final InputStream in = loadRequestStream("create.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            try {
                wrapper.handleRequest(in, out, context);
            } catch (final Error e) {
                // ignore so we can perform verifications
            }

            // verify initialiseRuntime was called and initialised dependencies
            verifyInitialiseRuntime();

            verify(providerMetricsPublisher, atLeastOnce()).publishExceptionByErrorCodeMetric(any(Instant.class),
                any(Action.class), any(HandlerErrorCode.class), any(Boolean.class));

            verify(providerMetricsPublisher).publishExceptionCountMetric(any(Instant.class), any(Action.class),
                any(Boolean.class));

            // no further calls to metrics publisher should occur
            verifyNoMoreInteractions(providerMetricsPublisher);

            // verify output response
            verifyHandlerResponse(out,
                ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                    .status(OperationStatus.FAILED).message("not an Exception")
                    .resourceModel(TestModel.builder().property1("abc").property2(123).build()).build());
        }
    }

    @Test
    public void invokeHandler_withInvalidPayload_returnsFailureResponse() throws IOException {
        try (final InputStream in = new ByteArrayInputStream(new byte[0]); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            try {
                wrapper.handleRequest(in, out, context);
            } catch (final Error e) {
                // ignore so we can perform verifications
            }

            // verify output response
            verifyHandlerResponse(out,
                ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                    .status(OperationStatus.FAILED).message("A JSONObject text must begin with '{' at 0 [character 1 line 1]")
                    .build());
        }
    }

    @Test
    public void invokeHandler_withNullInputStream_returnsFailureResponse() throws IOException {
        try (final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            try {
                wrapper.handleRequest(null, out, context);
            } catch (final Error e) {
                // ignore so we can perform verifications
            }

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                .status(OperationStatus.FAILED).message("No request object received").build());
        }
    }

    @Test
    public void invokeHandler_withEmptyPayload_returnsFailure() throws IOException {
        try (final InputStream in = loadRequestStream("empty.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            try {
                wrapper.handleRequest(in, out, context);
            } catch (final Error e) {
                // ignore so we can perform verifications
            }

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                .status(OperationStatus.FAILED).message("Invalid request object received").build());
        }
    }

    @Test
    public void invokeHandler_withEmptyResourceProperties_returnsFailure() throws IOException {
        try (final InputStream in = loadRequestStream("empty.resource.request.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            try {
                wrapper.handleRequest(in, out, context);
            } catch (final Error e) {
                // ignore so we can perform verifications
            }

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().errorCode(HandlerErrorCode.InternalFailure)
                .status(OperationStatus.FAILED).message("Invalid resource properties object received").build());
        }
    }

    @Test
    public void stringifiedPayload_validation_successful() throws IOException {
        // this test ensures that validation on the resource payload is performed
        // against the serialized
        // model rather than the raw payload. This allows the handlers to accept
        // incoming payloads that
        // may have quoted values where the JSON Serialization is still able to
        // construct a valid POJO
        SchemaValidator validator = new Validator();
        final WrapperOverride wrapper = new WrapperOverride(providerLoggingCredentialsProvider, platformEventsLogger,
                                                            providerEventsLogger, providerMetricsPublisher, validator,
                                                            httpClient);

        // explicit fault response is treated as an unsuccessful synchronous completion
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS).message("Handler was invoked").build();
        wrapper.setInvokeHandlerResponse(pe);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream("create.request-with-stringified-resource.json");
            final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            verifyHandlerResponse(out, ProgressEvent.<TestModel, TestContext>builder().message("Handler was invoked")
                .status(OperationStatus.SUCCESS).build());

        }
    }

    @Test
    public void getDesiredResourceTags_oneStackTagAndOneResourceTag() {
        final Map<String, String> stackTags = new HashMap<>();
        stackTags.put("Tag1", "Value1");

        final Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("Tag2", "Value2");
        final TestModel model = TestModel.builder().tags(resourceTags).build();

        final HandlerRequest<TestModel, TestContext> request = new HandlerRequest<>();
        final RequestData<TestModel> requestData = new RequestData<>();
        requestData.setResourceProperties(model);
        requestData.setStackTags(stackTags);
        request.setRequestData(requestData);

        final Map<String, String> tags = wrapper.getDesiredResourceTags(request);
        assertThat(tags).isNotNull();
        assertThat(tags.size()).isEqualTo(2);
        assertThat(tags.get("Tag1")).isEqualTo("Value1");
        assertThat(tags.get("Tag2")).isEqualTo("Value2");
    }

    @Test
    public void getDesiredResourceTags_resourceTagOverridesStackTag() {
        final Map<String, String> stackTags = new HashMap<>();
        stackTags.put("Tag1", "Value1");

        final Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("Tag1", "Value2");
        final TestModel model = TestModel.builder().tags(resourceTags).build();

        final HandlerRequest<TestModel, TestContext> request = new HandlerRequest<>();
        final RequestData<TestModel> requestData = new RequestData<>();
        requestData.setResourceProperties(model);
        requestData.setStackTags(stackTags);
        request.setRequestData(requestData);

        final Map<String, String> tags = wrapper.getDesiredResourceTags(request);
        assertThat(tags).isNotNull();
        assertThat(tags.size()).isEqualTo(1);
        assertThat(tags.get("Tag1")).isEqualTo("Value2");
    }

    @Test
    public void getPreviousResourceTags_oneStackTagAndOneResourceTag() {
        final Map<String, String> stackTags = new HashMap<>();
        stackTags.put("Tag1", "Value1");

        final Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("Tag2", "Value2");
        final TestModel model = TestModel.builder().tags(resourceTags).build();

        final HandlerRequest<TestModel, TestContext> request = new HandlerRequest<>();
        final RequestData<TestModel> requestData = new RequestData<>();
        requestData.setPreviousResourceProperties(model);
        requestData.setPreviousStackTags(stackTags);
        request.setRequestData(requestData);

        final Map<String, String> tags = wrapper.getPreviousResourceTags(request);
        assertThat(tags).isNotNull();
        assertThat(tags.size()).isEqualTo(2);
        assertThat(tags.get("Tag1")).isEqualTo("Value1");
        assertThat(tags.get("Tag2")).isEqualTo("Value2");
    }

    @Test
    public void getPreviousResourceTags_resourceTagOverridesStackTag() {
        final Map<String, String> stackTags = new HashMap<>();
        stackTags.put("Tag1", "Value1");

        final Map<String, String> resourceTags = new HashMap<>();
        resourceTags.put("Tag1", "Value2");
        final TestModel model = TestModel.builder().tags(resourceTags).build();

        final HandlerRequest<TestModel, TestContext> request = new HandlerRequest<>();
        final RequestData<TestModel> requestData = new RequestData<>();
        requestData.setPreviousResourceProperties(model);
        requestData.setPreviousStackTags(stackTags);
        request.setRequestData(requestData);

        final Map<String, String> tags = wrapper.getPreviousResourceTags(request);
        assertThat(tags).isNotNull();
        assertThat(tags.size()).isEqualTo(1);
        assertThat(tags.get("Tag1")).isEqualTo("Value2");
    }
}
