package com.amazonaws.cloudformation;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.cloudformation.exceptions.TerminalException;
import com.amazonaws.cloudformation.injection.CredentialsProvider;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;
import com.amazonaws.cloudformation.proxy.CallbackAdapter;
import com.amazonaws.cloudformation.proxy.Credentials;
import com.amazonaws.cloudformation.proxy.HandlerRequest;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.resource.SchemaValidator;
import com.amazonaws.cloudformation.resource.exceptions.ValidationException;
import com.amazonaws.cloudformation.scheduler.CloudWatchScheduler;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LambdaWrapperTest {

    private static final String TEST_DATA_BASE_PATH = "src/test/java/com/amazonaws/cloudformation/data/%s";

    @Mock
    private CallbackAdapter<TestModel> callbackAdapter;

    @Mock
    private CredentialsProvider credentialsProvider;

    @Mock
    private MetricsPublisher metricsPublisher;

    @Mock
    private CloudWatchScheduler scheduler;

    @Mock
    private SchemaValidator validator;

    @Mock
    private ResourceHandlerRequest<TestModel> resourceHandlerRequest;

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
        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);

        final Context context = mock(Context.class);
        lenient().when(context.getInvokedFunctionArn()).thenReturn("arn:aws:lambda:aws-region:acct-id:function:testHandler:PROD");
        when(context.getLogger()).thenReturn(lambdaLogger);

        return context;
    }

    private void testInvokeHandler_NullResponse(final String requestDataPath,
                                                final Action action) throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        // a null response is a terminal fault
        wrapper.setInvokeHandlerResponse(null);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // validation failure metric should be published for final error handling
            verify(metricsPublisher, times(1)).publishExceptionMetric(
                any(Instant.class), any(), any(TerminalException.class));

            // all metrics should be published even on terminal failure
            verify(metricsPublisher, times(1)).setResourceTypeName(
                "AWS::Test::TestModel");
            verify(metricsPublisher, times(1)).publishInvocationMetric(
                any(Instant.class), eq(action));
            verify(metricsPublisher, times(1)).publishDurationMetric(
                any(Instant.class), eq(action), anyLong());

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(
                    any(JSONObject.class), any(InputStream.class));
            }

            // no re-invocation via CloudWatch should occur
            verify(scheduler, times(0)).rescheduleAfterMinutes(
                anyString(), anyInt(), ArgumentMatchers.<HandlerRequest<TestModel, TestContext>>any());
            verify(scheduler, times(0)).cleanupCloudWatchEvents(
                any(), any());

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"FAILED\",\"bearerToken\":\"123456\",\"resourceModel\":{\"property2\":123,\"property1\":\"abc\"}," +
                "\"message\":\"Handler failed to provide a response.\"}");
        }
    }

    @Test
    public void testInvokeHandler_Create_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("create.request.json", Action.CREATE);
    }

    @Test
    public void testInvokeHandler_Read_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("read.request.json", Action.READ);
    }

    @Test
    public void testInvokeHandler_Update_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("update.request.json", Action.UPDATE);
    }

    @Test
    public void testInvokeHandler_Delete_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("delete.request.json", Action.DELETE);
    }

    @Test
    public void testInvokeHandler_List_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("list.request.json", Action.LIST);
    }

    private void testInvokeHandler_Failed(final String requestDataPath,
                                          final Action action) throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        // explicit fault response is treated as an unsuccessful synchronous completion
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.FAILED)
            .message("Custom Fault")
            .build();
        wrapper.setInvokeHandlerResponse(pe);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // all metrics should be published, once for a single invocation
            verify(metricsPublisher, times(1)).setResourceTypeName(
                "AWS::Test::TestModel");
            verify(metricsPublisher, times(1)).publishInvocationMetric(
                any(Instant.class), eq(action));
            verify(metricsPublisher, times(1)).publishDurationMetric(
                any(Instant.class), eq(action), anyLong());

            // validation failure metric should not be published
            verify(metricsPublisher, times(0)).publishExceptionMetric(
                any(Instant.class), any(), any(Exception.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(
                    any(JSONObject.class), any(InputStream.class));
            }

            // no re-invocation via CloudWatch should occur
            verify(scheduler, times(0)).rescheduleAfterMinutes(
                anyString(), anyInt(), ArgumentMatchers.<HandlerRequest<TestModel, TestContext>>any());
            verify(scheduler, times(0)).cleanupCloudWatchEvents(
                any(), any());

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"FAILED\",\"bearerToken\":\"123456\",\"message\":\"Custom Fault\"}");
        }
    }

    @Test
    public void testInvokeHandler_Create_Failed() throws IOException {
        testInvokeHandler_Failed("create.request.json", Action.CREATE);
    }

    @Test
    public void testInvokeHandler_Read_Failed() throws IOException {
        testInvokeHandler_Failed("read.request.json", Action.READ);
    }

    @Test
    public void testInvokeHandler_Update_Failed() throws IOException {
        testInvokeHandler_Failed("update.request.json", Action.UPDATE);
    }

    @Test
    public void testInvokeHandler_Delete_Failed() throws IOException {
        testInvokeHandler_Failed("delete.request.json", Action.DELETE);
    }

    @Test
    public void testInvokeHandler_List_Failed() throws IOException {
        testInvokeHandler_Failed("list.request.json", Action.LIST);
    }

    private void testInvokeHandler_CompleteSynchronously(final String requestDataPath,
                                                         final Action action) throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        // if the handler responds Complete, this is treated as a successful synchronous completion
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS)
            .build();
        wrapper.setInvokeHandlerResponse(pe);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);

        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // all metrics should be published, once for a single invocation
            verify(metricsPublisher, times(1)).setResourceTypeName(
                "AWS::Test::TestModel");
            verify(metricsPublisher, times(1)).publishInvocationMetric(
                any(Instant.class), eq(action));
            verify(metricsPublisher, times(1)).publishDurationMetric(
                any(Instant.class), eq(action), anyLong());

            // validation failure metric should not be published
            verify(metricsPublisher, times(0)).publishExceptionMetric(
                any(Instant.class), any(), any(Exception.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(
                    any(JSONObject.class), any(InputStream.class));
            }

            // no re-invocation via CloudWatch should occur
            verify(scheduler, times(0)).rescheduleAfterMinutes(
                anyString(), anyInt(), ArgumentMatchers.<HandlerRequest<TestModel, TestContext>>any());
            verify(scheduler, times(0)).cleanupCloudWatchEvents(
                any(), any());

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"SUCCESS\",\"bearerToken\":\"123456\"}");
        }
    }

    @Test
    public void testInvokeHandler_Create_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("create.request.json", Action.CREATE);
    }

    @Test
    public void testInvokeHandler_Read_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("read.request.json", Action.READ);
    }

    @Test
    public void testInvokeHandler_Update_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("update.request.json", Action.UPDATE);
    }

    @Test
    public void testInvokeHandler_Delete_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("delete.request.json", Action.DELETE);
    }

    @Test
    public void testInvokeHandler_List_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("list.request.json", Action.LIST);
    }

    private void testInvokeHandler_InProgress(final String requestDataPath,
                                              final Action action) throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
        wrapper.setInvokeHandlerResponse(pe);

        lenient().when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // all metrics should be published, once for a single invocation
            verify(metricsPublisher, times(1)).setResourceTypeName(
                "AWS::Test::TestModel");
            verify(metricsPublisher, times(1)).publishInvocationMetric(
                any(Instant.class), eq(action));
            verify(metricsPublisher, times(1)).publishDurationMetric(
                any(Instant.class), eq(action), anyLong());

            // validation failure metric should not be published
            verify(metricsPublisher, times(0)).publishExceptionMetric(
                any(Instant.class), any(), any(Exception.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(
                    any(JSONObject.class), any(InputStream.class));
            }

            // re-invocation via CloudWatch should occur
            verify(scheduler, times(1)).rescheduleAfterMinutes(
                anyString(), eq(0), ArgumentMatchers.<HandlerRequest<TestModel, TestContext>>any());

            // this was a first invocation, so no cleanup is required
            verify(scheduler, times(0)).cleanupCloudWatchEvents(
                any(), any());

            // CloudFormation should receive a callback invocation
            // TODO

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"IN_PROGRESS\",\"bearerToken\":\"123456\",\"resourceModel\":{}}");
        }
    }

    @Test
    public void testInvokeHandler_Create_InProgress() throws IOException {
        testInvokeHandler_InProgress("create.request.json", Action.CREATE);
    }

    @Test
    public void testInvokeHandler_Read_InProgress() throws IOException {
        testInvokeHandler_InProgress("read.request.json", Action.READ);
    }

    @Test
    public void testInvokeHandler_Update_InProgress() throws IOException {
        testInvokeHandler_InProgress("update.request.json", Action.UPDATE);
    }

    @Test
    public void testInvokeHandler_Delete_InProgress() throws IOException {
        testInvokeHandler_InProgress("delete.request.json", Action.DELETE);
    }

    @Test
    public void testInvokeHandler_List_InProgress() throws IOException {
        testInvokeHandler_InProgress("list.request.json", Action.LIST);
    }

    private void testReInvokeHandler_InProgress(final String requestDataPath,
                                                final Action action) throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
        wrapper.setInvokeHandlerResponse(pe);

        when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // all metrics should be published, once for a single invocation
            verify(metricsPublisher, times(1)).setResourceTypeName(
                "AWS::Test::TestModel");
            verify(metricsPublisher, times(1)).publishInvocationMetric(
                any(Instant.class), eq(action));
            verify(metricsPublisher, times(1)).publishDurationMetric(
                any(Instant.class), eq(action), anyLong());

            // validation failure metric should not be published
            verify(metricsPublisher, times(0)).publishExceptionMetric(
                any(Instant.class), any(), any(Exception.class));

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(
                    any(JSONObject.class), any(InputStream.class));
            }

            // re-invocation via CloudWatch should occur
            verify(scheduler, times(1)).rescheduleAfterMinutes(
                anyString(), eq(0), ArgumentMatchers.<HandlerRequest<TestModel, TestContext>>any());

            // this was a re-invocation, so a cleanup is required
            verify(scheduler, times(1)).cleanupCloudWatchEvents(
                eq("reinvoke-handler-4754ac8a-623b-45fe-84bc-f5394118a8be"),
                eq("reinvoke-target-4754ac8a-623b-45fe-84bc-f5394118a8be")
            );

            // CloudFormation should receive a callback invocation
            // TODO

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"IN_PROGRESS\",\"bearerToken\":\"123456\",\"resourceModel\":{}}");
        }
    }

    @Test
    public void testReInvokeHandler_Create_InProgress() throws IOException {
        testReInvokeHandler_InProgress("create.with-request-context.request.json", Action.CREATE);
    }

    @Test
    public void testReInvokeHandler_Read_InProgress() throws IOException {
        // TODO: READ handlers must return synchronously so this is probably a fault
        //testReInvokeHandler_InProgress("read.with-request-context.request.json", Action.READ);
    }

    @Test
    public void testReInvokeHandler_Update_InProgress() throws IOException {
        testReInvokeHandler_InProgress("update.with-request-context.request.json", Action.UPDATE);
    }

    @Test
    public void testReInvokeHandler_Delete_InProgress() throws IOException {
        testReInvokeHandler_InProgress("delete.with-request-context.request.json", Action.DELETE);
    }

    @Test
    public void testReInvokeHandler_List_InProgress() throws IOException {
        // TODO: LIST handlers must return synchronously so this is probably a fault
        //testReInvokeHandler_InProgress("list.with-request-context.request.json", Action.LIST);
    }

    private void testInvokeHandler_SchemaValidationFailure(final String requestDataPath,
                                                           final Action action) throws IOException {
        doThrow(ValidationException.class)
                .when(validator).validateObject(any(JSONObject.class), any(InputStream.class));
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        try (final InputStream in = loadRequestStream(requestDataPath); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // validation failure metric should be published but no others
            verify(metricsPublisher, times(1)).publishExceptionMetric(
                any(Instant.class), eq(action), any(Exception.class));

            // all metrics should be published, even for a single invocation
            verify(metricsPublisher, times(1)).setResourceTypeName(
                "AWS::Test::TestModel");
            verify(metricsPublisher, times(1)).publishInvocationMetric(
                any(Instant.class), eq(action));

            // duration metric only published when the provider handler is invoked
            verify(metricsPublisher, times(0)).publishDurationMetric(
                any(Instant.class), eq(action), anyLong());

            // verify that model validation occurred for CREATE/UPDATE/DELETE
            if (action == Action.CREATE || action == Action.UPDATE || action == Action.DELETE) {
                verify(validator, times(1)).validateObject(
                    any(JSONObject.class), any(InputStream.class));
            }

            // no re-invocation via CloudWatch should occur
            verify(scheduler, times(0)).rescheduleAfterMinutes(
                anyString(), anyInt(), ArgumentMatchers.<HandlerRequest<TestModel, TestContext>>any());
            verify(scheduler, times(0)).cleanupCloudWatchEvents(
                any(), any());

            // CloudFormation should receive a callback invocation
            // TODO

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"FAILED\",\"bearerToken\":\"123456\",\"resourceModel\":{\"property2\":123,\"property1\":\"abc\"}}");
        }
    }

    @Test
    public void testInvokeHandler_Create_SchemaValidationFailure() throws IOException {
        testInvokeHandler_SchemaValidationFailure("create.request.json", Action.CREATE);
    }

    @Test
    public void testInvokeHandler_Read_SchemaValidationFailure() throws IOException {
        // TODO: READ handlers must return synchronously so this is probably a fault
        //testReInvokeHandler_SchemaValidationFailure("read.with-request-context.request.json", Action.READ);
    }

    @Test
    public void testInvokeHandler_Update_SchemaValidationFailure() throws IOException {
        testInvokeHandler_SchemaValidationFailure("update.request.json", Action.UPDATE);
    }

    @Test
    public void testInvokeHandler_Delete_SchemaValidationFailure() throws IOException {
        testInvokeHandler_SchemaValidationFailure("delete.request.json", Action.DELETE);
    }

    @Test
    public void testInvokeHandler_List_SchemaValidationFailure() throws IOException {
        // TODO: LIST handlers must return synchronously so this is probably a fault
        //testInvokeHandler_SchemaValidationFailure("list.with-request-context.request.json", Action.LIST);
    }

    @Test
    public void testInvokeHandler_WithMalformedRequest() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS)
            .resourceModel(model)
            .build();
        wrapper.setInvokeHandlerResponse(pe);

        when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);


        // our ObjectMapper implementation will ignore extraneous fields rather than fail them
        // this slightly loosens the coupling between caller (CloudFormation) and handlers.
        try (final InputStream in = loadRequestStream("malformed.request.json"); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"SUCCESS\",\"bearerToken\":\"123456\",\"resourceModel\":{}}");
        }
    }

    @Test
    public void testInvokeHandler_WithoutPlatformCredentials() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        // without platform credentials the handler is unable to do
        // basic SDK initialization and any such request should fail fast
        try (final InputStream in = loadRequestStream("create.request-without-platform-credentials.json"); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"FAILED\",\"bearerToken\":\"123456\",\"resourceModel\":{\"property2\":123,\"property1\":\"abc\"},\"message\":\"Missing required platform credentials\"}");
        }
    }

    @Test
    public void testInvokeHandler_WithDefaultInjection() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();
        model.setProperty1("abc");
        model.setProperty2(123);
        when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        wrapper.setTransformResponse(resourceHandlerRequest);

        // respond with immediate success to avoid callback invocation
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS)
            .resourceModel(model)
            .build();
        wrapper.setInvokeHandlerResponse(pe);

        // without platform credentials the handler is unable to do
        // basic SDK initialization and any such request should fail fast
        try (final InputStream in = loadRequestStream("create.request.json"); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"SUCCESS\",\"bearerToken\":\"123456\",\"resourceModel\":{\"property2\":123,\"property1\":\"abc\"}}");
        }
    }

    @Test
    public void testFailToRescheduleInvocation() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();
        model.setProperty1("abc");
        model.setProperty2(123);
        when(resourceHandlerRequest.getDesiredResourceState()).thenReturn(model);
        doThrow(new AmazonServiceException("Throttled")).when(scheduler).rescheduleAfterMinutes(anyString(), anyInt(), ArgumentMatchers.<HandlerRequest<TestModel, TestContext>>any());
        wrapper.setTransformResponse(resourceHandlerRequest);

        // respond with in progress status to trigger callback invocation
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(model)
            .build();
        wrapper.setInvokeHandlerResponse(pe);

        try (final InputStream in = loadRequestStream("create.request.json"); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // verify output response
            assertThat(out.toString()).isEqualTo(
                "{\"operationStatus\":\"FAILED\",\"bearerToken\":\"123456\",\"resourceModel\":{\"property2\":123,\"property1\":\"abc\"},\"errorCode\":\"ServiceException\",\"message\":\"Throttled (Service: null; Status Code: 0; Error Code: null; Request ID: null)\"}");
        }
    }

    @Test
    public void testClientsRefreshedOnEveryInvoke() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);

        Context context = getLambdaContext();
        try (InputStream in = loadRequestStream("create.request.json"); OutputStream out = new ByteArrayOutputStream()) {
            wrapper.handleRequest(in, out, context);
        }

        verify(callbackAdapter, times(1)).refreshClient();
        verify(metricsPublisher, times(1)).refreshClient();
        verify(scheduler, times(1)).refreshClient();

        // invoke the same wrapper instance again to ensure client is refreshed
        context = getLambdaContext();
        try (InputStream in = loadRequestStream("create.request.json"); OutputStream out = new ByteArrayOutputStream()) {
            wrapper.handleRequest(in, out, context);
        }

        verify(callbackAdapter, times(2)).refreshClient();
        verify(metricsPublisher, times(2)).refreshClient();
        verify(scheduler, times(2)).refreshClient();
    }

    @Test
    public void testPlatformCredentialsRefreshedOnEveryInvoke() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);

        Context context = getLambdaContext();
        try (InputStream in = loadRequestStream("create.request.json"); OutputStream out = new ByteArrayOutputStream()) {
            wrapper.handleRequest(in, out, context);
        }

        final Credentials expected = new Credentials(
            "32IEHAHFIAG538KYASAI",
            "0O2hop/5vllVHjbA8u52hK8rLcroZpnL5NPGOi66",
            "gqe6eIsFPHOlfhc3RKl5s5Y6Dy9PYvN1CEYsswz5TQUsE8WfHD6LPK549euXm4Vn4INBY9nMJ1cJe2mxTYFdhWHSnkOQv2SHemal"
        );
        verify(credentialsProvider, times(1)).setCredentials(eq(expected));

        // invoke the same wrapper instance again to ensure client is refreshed
        context = getLambdaContext();
        try (InputStream in = loadRequestStream("create.request.with-new-credentials.json"); OutputStream out = new ByteArrayOutputStream()) {
            wrapper.handleRequest(in, out, context);
        }

        final Credentials expectedNew = new Credentials(
            "GT530IJDHALYZQSZZ8XG",
            "UeJEwC/dqcYEn2viFd5TjKjR5TaMOfdeHrlLXxQL",
            "469gs8raWJCaZcItXhGJ7dt3urI13fOTcde6ibhuHJz6r6bRRCWvLYGvCsqrN8WUClYL9lxZHymrWXvZ9xN0GoI2LFdcAAinZk5t"
        );

        verify(credentialsProvider, times(1)).setCredentials(eq(expectedNew));
    }

    @Test
    public void testInvokeHandler_WithNoResponseEndpoint() throws IOException {
        final WrapperOverride wrapper = new WrapperOverride(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator);
        final TestModel model = new TestModel();

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel, TestContext> pe = ProgressEvent.<TestModel, TestContext>builder()
            .status(OperationStatus.SUCCESS)
            .resourceModel(model)
            .build();
        wrapper.setInvokeHandlerResponse(pe);
        wrapper.setTransformResponse(resourceHandlerRequest);

        // our ObjectMapper implementation will ignore extraneous fields rather than fail them
        // this slightly loosens the coupling between caller (CloudFormation) and handlers.
        try (final InputStream in = loadRequestStream("no-response-endpoint.request.json"); final OutputStream out = new ByteArrayOutputStream()) {
            final Context context = getLambdaContext();

            wrapper.handleRequest(in, out, context);

            // malformed input exception is published
            verify(metricsPublisher, times(1)).publishExceptionMetric(
                any(Instant.class), any(), any(TerminalException.class));

            // verify output response
            assertThat(out.toString()).isEqualTo("{\"operationStatus\":\"FAILED\",\"bearerToken\":\"123456\",\"resourceModel\":{\"property2\":123,\"property1\":\"abc\"},\"message\":\"No callback endpoint received\"}");
        }
    }
}
