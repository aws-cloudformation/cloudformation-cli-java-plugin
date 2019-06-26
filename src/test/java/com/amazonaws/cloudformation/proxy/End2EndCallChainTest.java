package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.Response;
import com.amazonaws.cloudformation.injection.CloudWatchEventsProvider;
import com.amazonaws.cloudformation.injection.CredentialsProvider;
import com.amazonaws.cloudformation.loggers.LogPublisher;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;
import com.amazonaws.cloudformation.proxy.handler.Model;
import com.amazonaws.cloudformation.proxy.handler.ServiceHandlerWrapper;
import com.amazonaws.cloudformation.proxy.service.AccessDenied;
import com.amazonaws.cloudformation.proxy.service.CreateRequest;
import com.amazonaws.cloudformation.proxy.service.CreateResponse;
import com.amazonaws.cloudformation.proxy.service.DescribeRequest;
import com.amazonaws.cloudformation.proxy.service.DescribeResponse;
import com.amazonaws.cloudformation.proxy.service.ExistsException;
import com.amazonaws.cloudformation.proxy.service.NotFoundException;
import com.amazonaws.cloudformation.proxy.service.ServiceClient;
import com.amazonaws.cloudformation.proxy.service.ThrottleException;
import com.amazonaws.cloudformation.resource.Serializer;
import com.amazonaws.cloudformation.resource.Validator;
import com.amazonaws.cloudformation.scheduler.CloudWatchScheduler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import org.joda.time.Instant;
import org.json.JSONObject;

// Testing needs
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.time.Duration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class End2EndCallChainTest {

    static final AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);

    //
    // The same that is asserted inside the ServiceClient
    //
    private final AwsSessionCredentials MockCreds = AwsSessionCredentials.create(
        "accessKeyId", "secretKey", "token");
    private final Credentials credentials =
       new Credentials(MockCreds.accessKeyId(), MockCreds.secretAccessKey(), MockCreds.sessionToken());
    @SuppressWarnings("unchecked")
    private final CallbackAdapter<Model> adapter = mock(CallbackAdapter.class);

    @Test
    public void happyCase() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            credentials,
            () -> Duration.ofMinutes(10).getSeconds());
        final Model model = Model.builder().repoName("repo").build();
        StdCallbackContext context = new StdCallbackContext();
        final ServiceClient serviceClient = mock(ServiceClient.class);
        when(serviceClient.createRepository(any(CreateRequest.class))).thenReturn(
            new CreateResponse.Builder()
                .repoName(model.getRepoName()).build());

        ProxyClient<ServiceClient> client = proxy.newProxy(() -> serviceClient);

        ProgressEvent<Model, StdCallbackContext> event =
            proxy.initiate("client:createRepository", client, model, context)
            .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
            .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
            .done(r -> ProgressEvent.success(model, context));

        assertThat(event.getStatus()).isEqualTo(OperationStatus.SUCCESS);

        // replay, should get the same result.
        event =
            proxy.initiate("client:createRepository", client, model, context)
            .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
            .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
            .done(r -> ProgressEvent.success(model, context));
        //
        // Verify that we only got called. During replay we should have been skipped.
        //
        verify(serviceClient).createRepository(any(CreateRequest.class));

        assertThat(event.getStatus()).isEqualTo(OperationStatus.SUCCESS);

        // Now a separate request
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(500);

        //
        // Now reset expectation to fail with already exists
        //
        final ExistsException exists =
            new ExistsException(mock(AwsServiceException.Builder.class)) {
                private static final long serialVersionUID = 1L;
                @Override
                public AwsErrorDetails awsErrorDetails() {
                    return AwsErrorDetails.builder()
                        .errorCode("AlreadyExists")
                        .errorMessage("Repo already exists")
                        .sdkHttpResponse(sdkHttpResponse)
                        .build();
                }
            };
        when(serviceClient.createRepository(any(CreateRequest.class))).thenThrow(exists);
        StdCallbackContext newContext = new StdCallbackContext();
        event =
            proxy.initiate("client:createRepository", client, model, newContext)
                .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .done(r -> ProgressEvent.success(model, context));

        assertThat(event.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(event.getMessage()).contains("AlreadyExists");
    }

    private HandlerRequest<Model, StdCallbackContext> prepareRequest(Model model) throws Exception {
        HandlerRequest<Model, StdCallbackContext> request = new HandlerRequest<>();
        request.setAction(Action.CREATE);
        request.setAwsAccountId("1234567891234");
        request.setBearerToken("dwezxdfgfgh");
        request.setNextToken(null);
        request.setRegion("us-east-2");
        request.setResourceType("AWS::Code::Repository");
        request.setStackId(UUID.randomUUID().toString());
        request.setResponseEndpoint("https://cloudformation.amazonaws.com");
        RequestData<Model> data = new RequestData<>();
        data.setResourceProperties(model);
        data.setPlatformCredentials(credentials);
        data.setCallerCredentials(credentials);
        request.setRequestData(data);
        return request;
    }

    private HandlerRequest<Model, StdCallbackContext> prepareRequest() throws Exception {
        return prepareRequest(Model.builder().repoName("repository").build());
    }

    private InputStream prepareStream(Serializer serializer,
                                      HandlerRequest<Model, StdCallbackContext> request) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        JSONObject value = serializer.serialize(request);
        value.write(writer);
        writer.flush();
        writer.close();

        return new ByteArrayInputStream(out.toByteArray());
    }

    private CredentialsProvider prepareMockProvider() {
        return
            new CredentialsProvider() {
                @Override
                public AwsSessionCredentials get() {
                    return MockCreds;
                }

                @Override
                public void setCredentials(Credentials credentials) {

                }
            };
    }

    @Order(5)
    @Test
    public void notFound() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest(
            Model.builder().repoName("repository").build());
        request.setAction(Action.READ);
        final Model model = request.getRequestData().getResourceProperties();
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final CredentialsProvider platformCredentialsProvider = prepareMockProvider();
        final CredentialsProvider resourceOwnerLoggingCredentialsProvider = prepareMockProvider();
        final Context cxt = mock(Context.class);
        // bail out immediately
        when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        when(cxt.getLogger()).thenReturn(mock(LambdaLogger.class));

        final ServiceClient client = mock(ServiceClient.class);
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(404);
        final DescribeRequest describeRequest =
            new DescribeRequest.Builder().repoName(model.getRepoName())
                .overrideConfiguration(
                    AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(
                            StaticCredentialsProvider.create(MockCreds))
                        .build()
                ).build();
        final NotFoundException notFound =
            new NotFoundException(mock(AwsServiceException.Builder.class)) {
                private static final long serialVersionUID = 1L;
                @Override
                public AwsErrorDetails awsErrorDetails() {
                    return
                        AwsErrorDetails.builder()
                            .errorCode("NotFound")
                            .errorMessage("Repo not existing")
                            .sdkHttpResponse(sdkHttpResponse)
                            .build();
                }
            };
        when(client.describeRepository(eq(describeRequest))).thenThrow(notFound);

        final ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
                adapter,
                platformCredentialsProvider,
                resourceOwnerLoggingCredentialsProvider,
                mock(LogPublisher.class),
                mock(LogPublisher.class),
                mock(MetricsPublisher.class),
                mock(MetricsPublisher.class),
                new CloudWatchScheduler(
                        new CloudWatchEventsProvider(platformCredentialsProvider) {
                            @Override
                            public CloudWatchEventsClient get() {
                                return mock(CloudWatchEventsClient.class);
                            }
                        },
                        loggerProxy),
                new Validator(),
                serializer,
                client
        );

        wrapper.handleRequest(stream, output, cxt);
        verify(client).describeRepository(eq(describeRequest));

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        assertThat(response).isNotNull();
        assertThat(response.getOperationStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getBearerToken()).isEqualTo("dwezxdfgfgh");
        assertThat(response.getMessage()).contains("NotFound");
    }

    @SuppressWarnings("unchecked")
    @Order(10)
    @Test
    public void createHandler() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest();
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final CredentialsProvider platformCredentialsProvider = prepareMockProvider();
        final CredentialsProvider resourceOwnerLoggingCredentialsProvider = prepareMockProvider();
        final Context cxt = mock(Context.class);
        // bail out immediately
        when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        when(cxt.getLogger()).thenReturn(mock(LambdaLogger.class));

        final Model model = request.getRequestData().getResourceProperties();

        final ServiceClient client = mock(ServiceClient.class);
        final CreateRequest createRequest = new CreateRequest.Builder()
            .repoName(model.getRepoName())
            .overrideConfiguration(
                AwsRequestOverrideConfiguration.builder()
                    .credentialsProvider(
                        StaticCredentialsProvider.create(MockCreds))
                    .build()
            )
            .build();
        when(client.createRepository(eq(createRequest))).thenReturn(
            new CreateResponse.Builder().repoName(model.getRepoName()).build());

        final DescribeRequest describeRequest =
            new DescribeRequest.Builder().repoName(model.getRepoName())
                .overrideConfiguration(
                    AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(
                            StaticCredentialsProvider.create(MockCreds))
                        .build()
                ).build();
        final DescribeResponse describeResponse =
            new DescribeResponse.Builder()
                .createdWhen(Instant.now().toDate())
                .repoName(model.getRepoName())
                .repoArn("some-arn")
                .build();
        when(client.describeRepository(eq(describeRequest))).thenReturn(describeResponse);

        final ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
            adapter,
            platformCredentialsProvider,
            resourceOwnerLoggingCredentialsProvider,
            mock(LogPublisher.class),
            mock(LogPublisher.class),
            mock(MetricsPublisher.class),
            mock(MetricsPublisher.class),
            new CloudWatchScheduler(
                new CloudWatchEventsProvider(platformCredentialsProvider) {
                    @Override
                    public CloudWatchEventsClient get() {
                        return mock(CloudWatchEventsClient.class);
                    }
                },
                loggerProxy),
            new Validator(),
            serializer,
           client
        );

        wrapper.handleRequest(stream, output, cxt);
        verify(client).createRepository(eq(createRequest));
        verify(client).describeRepository(eq(describeRequest));

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        assertThat(response).isNotNull();
        assertThat(response.getOperationStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getBearerToken()).isEqualTo("dwezxdfgfgh");
        assertThat(request.getRequestData()).isNotNull();
        Model responseModel = response.getResourceModel();
        assertThat(responseModel.getRepoName()).isEqualTo("repository");
        assertThat(responseModel.getArn()).isNotNull();
        assertThat(responseModel.getCreated()).isNotNull();
    }

    @Order(20)
    @Test
    public void createHandlerAlreadyExists() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest();
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final CredentialsProvider platformCredentialsProvider = prepareMockProvider();
        final CredentialsProvider resourceOwnerLoggingCredentialsProvider = prepareMockProvider();
        final Context cxt = mock(Context.class);
        // bail out immediately
        when(cxt.getRemainingTimeInMillis()).thenReturn((int)Duration.ofMinutes(1).toMillis());
        when(cxt.getLogger()).thenReturn(mock(LambdaLogger.class));

        final Model model = request.getRequestData().getResourceProperties();
        final ServiceClient client = mock(ServiceClient.class);
        final CreateRequest createRequest = new CreateRequest.Builder()
            .repoName(model.getRepoName())
            .overrideConfiguration(
                AwsRequestOverrideConfiguration.builder()
                    .credentialsProvider(
                        StaticCredentialsProvider.create(MockCreds))
                    .build()
            )
            .build();
        // Now a separate request
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(500);

        final ExistsException exists = new ExistsException(mock(AwsServiceException.Builder.class)) {
            private static final long serialVersionUID = 1L;
            @Override
            public AwsErrorDetails awsErrorDetails() {
                return AwsErrorDetails.builder()
                    .errorCode("AlreadyExists")
                    .errorMessage("Repo already exists")
                    .sdkHttpResponse(sdkHttpResponse)
                    .build();
            }
        };
        when(client.createRepository(eq(createRequest))).thenThrow(exists);

        final ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
                adapter,
                platformCredentialsProvider,
                resourceOwnerLoggingCredentialsProvider,
                mock(LogPublisher.class),
                mock(LogPublisher.class),
                mock(MetricsPublisher.class),
                mock(MetricsPublisher.class),
                new CloudWatchScheduler(
                        new CloudWatchEventsProvider(platformCredentialsProvider) {
                            @Override
                            public CloudWatchEventsClient get() {
                                return mock(CloudWatchEventsClient.class);
                            }
                        },
                        loggerProxy),
                new Validator(),
                serializer,
                client
        );

        wrapper.handleRequest(stream, output, cxt);
        verify(client).createRepository(eq(createRequest));

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        assertThat(response).isNotNull();
        assertThat(response.getOperationStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getBearerToken()).isEqualTo("dwezxdfgfgh");
        assertThat(request.getRequestData()).isNotNull();
        Model responseModel = response.getResourceModel();
        assertThat(responseModel.getRepoName()).isEqualTo("repository");
        assertThat(response.getMessage()).contains("AlreadyExists");
    }

    @Order(30)
    @Test
    public void createHandlerThottleException() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest(
            Model.builder().repoName("repository").build());
        request.setAction(Action.READ);
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final CredentialsProvider platformCredentialsProvider = prepareMockProvider();
        final CredentialsProvider resourceOwnerLoggingCredentialsProvider = prepareMockProvider();
        final Context cxt = mock(Context.class);
        // bail out very slowly
        when(cxt.getRemainingTimeInMillis()).thenReturn((int)Duration.ofMinutes(1).toMillis());
        when(cxt.getLogger()).thenReturn(mock(LambdaLogger.class));

        final Model model = request.getRequestData().getResourceProperties();
        final ServiceClient client = mock(ServiceClient.class);
        // Now a separate request
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(429);
        final DescribeRequest describeRequest =
            new DescribeRequest.Builder().repoName(model.getRepoName())
                .overrideConfiguration(
                    AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(
                            StaticCredentialsProvider.create(MockCreds))
                        .build()
                ).build();
        final ThrottleException throttleException = new ThrottleException(mock(AwsServiceException.Builder.class)) {
            private static final long serialVersionUID = 1L;
            @Override
            public AwsErrorDetails awsErrorDetails() {
                return AwsErrorDetails.builder()
                    .errorCode("ThrottleException")
                    .errorMessage("Temporary Limit Exceeded")
                    .sdkHttpResponse(sdkHttpResponse)
                    .build();
            }

        };
        when(client.describeRepository(eq(describeRequest))).thenThrow(throttleException);

        final ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
                adapter,
                platformCredentialsProvider,
                resourceOwnerLoggingCredentialsProvider,
                mock(LogPublisher.class),
                mock(LogPublisher.class),
                mock(MetricsPublisher.class),
                mock(MetricsPublisher.class),
                new CloudWatchScheduler(
                        new CloudWatchEventsProvider(platformCredentialsProvider) {
                            @Override
                            public CloudWatchEventsClient get() {
                                return mock(CloudWatchEventsClient.class);
                            }
                        },
                        loggerProxy),
                new Validator(),
                serializer,
                client
        );

        wrapper.handleRequest(stream, output, cxt);
        // Throttle retries 4 times (1, 0s), (2, 3s), (3, 6s), (4, 9s)
        verify(client, times(4)).describeRepository(eq(describeRequest));

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        assertThat(response).isNotNull();
        assertThat(response.getOperationStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getBearerToken()).isEqualTo("dwezxdfgfgh");
        assertThat(response.getMessage()).contains("Exceeded");
    }

    @Order(40)
    @Test
    public void createHandlerThottleExceptionEarlyInProgressBailout() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest(
            Model.builder().repoName("repository").build());
        request.setAction(Action.READ);
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final CredentialsProvider platformCredentialsProvider = prepareMockProvider();
        final CredentialsProvider resourceOwnerLoggingCredentialsProvider = prepareMockProvider();
        final Context cxt = mock(Context.class);
        // bail out immediately
        when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        when(cxt.getLogger()).thenReturn(mock(LambdaLogger.class));

        final Model model = request.getRequestData().getResourceProperties();
        final ServiceClient client = mock(ServiceClient.class);
        // Now a separate request
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(429);
        final DescribeRequest describeRequest =
            new DescribeRequest.Builder().repoName(model.getRepoName())
                .overrideConfiguration(
                    AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(
                            StaticCredentialsProvider.create(MockCreds))
                        .build()
                ).build();
        final ThrottleException throttleException = new ThrottleException(mock(AwsServiceException.Builder.class)) {
            private static final long serialVersionUID = 1L;
            @Override
            public AwsErrorDetails awsErrorDetails() {
                return AwsErrorDetails.builder()
                    .errorCode("ThrottleException")
                    .errorMessage("Temporary Limit Exceeded")
                    .sdkHttpResponse(sdkHttpResponse)
                    .build();
            }

        };
        when(client.describeRepository(eq(describeRequest))).thenThrow(throttleException);

        final ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
                adapter,
                platformCredentialsProvider,
                resourceOwnerLoggingCredentialsProvider,
                mock(LogPublisher.class),
                mock(LogPublisher.class),
                mock(MetricsPublisher.class),
                mock(MetricsPublisher.class),
                new CloudWatchScheduler(
                        new CloudWatchEventsProvider(platformCredentialsProvider) {
                            @Override
                            public CloudWatchEventsClient get() {
                                return mock(CloudWatchEventsClient.class);
                            }
                        },
                        loggerProxy),
                new Validator(),
                serializer,
                client
        );

        wrapper.handleRequest(stream, output, cxt);
        // only 1 call (1, 0s), the next attempt is at 3s which exceed 50 ms remaining
        verify(client).describeRepository(eq(describeRequest));

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        assertThat(response).isNotNull();
        assertThat(response.getOperationStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getBearerToken()).isEqualTo("dwezxdfgfgh");
    }

    @Order(40)
    @Test
    public void accessDenied() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest(
            Model.builder().repoName("repository").build());
        request.setAction(Action.READ);
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final CredentialsProvider platformCredentialsProvider = prepareMockProvider();
        final CredentialsProvider resourceOwnerLoggingCredentialsProvider = prepareMockProvider();
        final Context cxt = mock(Context.class);
        // bail out immediately
        when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        when(cxt.getLogger()).thenReturn(mock(LambdaLogger.class));

        final Model model = request.getRequestData().getResourceProperties();
        final ServiceClient client = mock(ServiceClient.class);
        // Now a separate request
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(401);
        final DescribeRequest describeRequest =
            new DescribeRequest.Builder().repoName(model.getRepoName())
                .overrideConfiguration(
                    AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(
                            StaticCredentialsProvider.create(MockCreds))
                        .build()
                ).build();
        final AccessDenied accessDenied = new AccessDenied(mock(AwsServiceException.Builder.class)) {
            private static final long serialVersionUID = 1L;
            @Override
            public AwsErrorDetails awsErrorDetails() {
                return AwsErrorDetails.builder()
                    .errorCode("AccessDenied: 401")
                    .errorMessage("Token Invalid")
                    .sdkHttpResponse(sdkHttpResponse)
                    .build();
            }

        };
        when(client.describeRepository(eq(describeRequest))).thenThrow(accessDenied);

        final ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
                adapter,
                platformCredentialsProvider,
                resourceOwnerLoggingCredentialsProvider,
                mock(LogPublisher.class),
                mock(LogPublisher.class),
                mock(MetricsPublisher.class),
                mock(MetricsPublisher.class),
                new CloudWatchScheduler(
                        new CloudWatchEventsProvider(platformCredentialsProvider) {
                            @Override
                            public CloudWatchEventsClient get() {
                                return mock(CloudWatchEventsClient.class);
                            }
                        },
                        loggerProxy),
                new Validator(),
                serializer,
                client
        );

        wrapper.handleRequest(stream, output, cxt);
        verify(client).describeRepository(eq(describeRequest));

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        assertThat(response).isNotNull();
        assertThat(response.getOperationStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getBearerToken()).isEqualTo("dwezxdfgfgh");
        assertThat(response.getMessage()).contains("AccessDenied");
    }

}
