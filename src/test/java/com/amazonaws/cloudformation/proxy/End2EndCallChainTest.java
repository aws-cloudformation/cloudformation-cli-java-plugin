package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.Response;
import com.amazonaws.cloudformation.injection.CloudWatchEventsProvider;
import com.amazonaws.cloudformation.injection.CredentialsProvider;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;
import com.amazonaws.cloudformation.proxy.handler.Model;
import com.amazonaws.cloudformation.proxy.handler.ServiceHandlerWrapper;
import com.amazonaws.cloudformation.proxy.service.CreateRequest;
import com.amazonaws.cloudformation.proxy.service.ServiceClient;
import com.amazonaws.cloudformation.resource.Serializer;
import com.amazonaws.cloudformation.resource.Validator;
import com.amazonaws.cloudformation.scheduler.CloudWatchScheduler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.time.Duration;

//
// This
//
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class End2EndCallChainTest {

    static final AwsServiceException.Builder builder = Mockito.mock(AwsServiceException.Builder.class);

    //
    // The same that is asserted inside the ServiceClient
    //
    private final AwsSessionCredentials MockCreds = AwsSessionCredentials.create(
        "accessKeyId", "secretKey", "token");
    private final Credentials credentials =
       new Credentials(MockCreds.accessKeyId(), MockCreds.secretAccessKey(), MockCreds.sessionToken());
    @SuppressWarnings("unchecked")
    private final CallbackAdapter<Model> adapter = Mockito.mock(CallbackAdapter.class);

    private final ServiceClient client = new ServiceClient();

    @Test
    public void happyCase() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            Mockito.mock(LambdaLogger.class),
            credentials,
            () -> (int)Duration.ofMinutes(10).getSeconds());
        Model model = Model.builder().repoName("repo").build();
        StdCallbackContext context = new StdCallbackContext();
        ProxyClient<ServiceClient> client = proxy.newProxy(() -> this.client);
        ProgressEvent<Model, StdCallbackContext> event =
            proxy.initiate("client:createRepository", client, model, context)
            .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
            .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
            .done(r -> ProgressEvent.success(model, context));

        Assertions.assertEquals(OperationStatus.SUCCESS, event.getStatus());

        // replay, should get the same result.
        event =
            proxy.initiate("client:createRepository", client, model, context)
            .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
            .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
            .done(r -> ProgressEvent.success(model, context));

        Assertions.assertEquals(OperationStatus.SUCCESS, event.getStatus());

        // Now a separate request
        StdCallbackContext newContext = new StdCallbackContext();
        event =
            proxy.initiate("client:createRepository", client, model, newContext)
                .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .done(r -> ProgressEvent.success(model, context));

        Assertions.assertEquals(OperationStatus.FAILED, event.getStatus());
        Assertions.assertTrue(event.getMessage().contains("AlreadyExists"));
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
            Model.builder().repoName("repository").throttle(true).build());
        request.setAction(Action.READ);
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        final CredentialsProvider provider = prepareMockProvider();
        Context cxt = Mockito.mock(Context.class);
        // bail out immediately
        Mockito.when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        Mockito.when(cxt.getLogger()).thenReturn(logger);

        ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
            adapter,
            provider,
            Mockito.mock(MetricsPublisher.class),
            new CloudWatchScheduler(
                new CloudWatchEventsProvider(provider) {
                    @Override
                    public CloudWatchEventsClient get() {
                        return Mockito.mock(CloudWatchEventsClient.class);
                    }
                },
                logger),
            new Validator(),
            serializer
        );

        wrapper.handleRequest(stream, output, cxt);

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(OperationStatus.FAILED, response.getOperationStatus());
        Assertions.assertEquals("dwezxdfgfgh", response.getBearerToken());
        Assertions.assertTrue(response.getMessage().contains("NotFound"));

    }

    @SuppressWarnings("unchecked")
    @Order(10)
    @Test
    public void createHandler() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest();
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        final CredentialsProvider provider = prepareMockProvider();
        Context cxt = Mockito.mock(Context.class);
        // bail out immediately
        Mockito.when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        Mockito.when(cxt.getLogger()).thenReturn(logger);

        ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
            adapter,
            provider,
            Mockito.mock(MetricsPublisher.class),
            new CloudWatchScheduler(
                new CloudWatchEventsProvider(provider) {
                    @Override
                    public CloudWatchEventsClient get() {
                        return Mockito.mock(CloudWatchEventsClient.class);
                    }
                },
                logger),
            new Validator(),
            serializer
        );

        wrapper.handleRequest(stream, output, cxt);

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(OperationStatus.SUCCESS, response.getOperationStatus());
        Assertions.assertEquals("dwezxdfgfgh", response.getBearerToken());
        Assertions.assertNotNull(request.getRequestData());
        Model responseModel = response.getResourceModel();
        Assertions.assertEquals("repository", responseModel.getRepoName());
        Assertions.assertNotNull(responseModel.getArn());
        Assertions.assertNotNull(responseModel.getCreated());
    }

    @Order(20)
    @Test
    public void createHandlerAlreadyExists() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest();
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        final CredentialsProvider provider = prepareMockProvider();
        Context cxt = Mockito.mock(Context.class);
        // bail out immediately
        Mockito.when(cxt.getRemainingTimeInMillis()).thenReturn((int)Duration.ofMinutes(1).toMillis());
        Mockito.when(cxt.getLogger()).thenReturn(logger);

        ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
            adapter,
            provider,
            Mockito.mock(MetricsPublisher.class),
            new CloudWatchScheduler(
                new CloudWatchEventsProvider(provider) {
                    @Override
                    public CloudWatchEventsClient get() {
                        return Mockito.mock(CloudWatchEventsClient.class);
                    }
                },
                logger),
            new Validator(),
            serializer
        );

        wrapper.handleRequest(stream, output, cxt);

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(OperationStatus.FAILED, response.getOperationStatus());
        Assertions.assertEquals("dwezxdfgfgh", response.getBearerToken());
        Assertions.assertNotNull(request.getRequestData());
        Model responseModel = response.getResourceModel();
        Assertions.assertEquals("repository", responseModel.getRepoName());
        Assertions.assertTrue(response.getMessage().contains("AlreadyExists"));
    }

    @Order(30)
    @Test
    public void createHandlerThottleException() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest(
            Model.builder().repoName("repository").throttle(true).build());
        request.setAction(Action.READ);
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        final CredentialsProvider provider = prepareMockProvider();
        Context cxt = Mockito.mock(Context.class);
        // bail out immediately
        Mockito.when(cxt.getRemainingTimeInMillis()).thenReturn((int)Duration.ofMinutes(1).toMillis());
        Mockito.when(cxt.getLogger()).thenReturn(logger);

        ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
            adapter,
            provider,
            Mockito.mock(MetricsPublisher.class),
            new CloudWatchScheduler(
                new CloudWatchEventsProvider(provider) {
                    @Override
                    public CloudWatchEventsClient get() {
                        return Mockito.mock(CloudWatchEventsClient.class);
                    }
                },
                logger),
            new Validator(),
            serializer
        );

        wrapper.handleRequest(stream, output, cxt);

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(OperationStatus.FAILED, response.getOperationStatus());
        Assertions.assertEquals("dwezxdfgfgh", response.getBearerToken());
        Assertions.assertTrue(response.getMessage().contains("Exceeded"));
    }

    @Order(40)
    @Test
    public void createHandlerThottleExceptionEarlyInProgressBailout() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest(
            Model.builder().repoName("repository").throttle(true).build());
        request.setAction(Action.READ);
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        final CredentialsProvider provider = prepareMockProvider();
        Context cxt = Mockito.mock(Context.class);
        // bail out immediately
        Mockito.when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        Mockito.when(cxt.getLogger()).thenReturn(logger);

        ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
            adapter,
            provider,
            Mockito.mock(MetricsPublisher.class),
            new CloudWatchScheduler(
                new CloudWatchEventsProvider(provider) {
                    @Override
                    public CloudWatchEventsClient get() {
                        return Mockito.mock(CloudWatchEventsClient.class);
                    }
                },
                logger),
            new Validator(),
            serializer
        );

        wrapper.handleRequest(stream, output, cxt);

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(OperationStatus.IN_PROGRESS, response.getOperationStatus());
        Assertions.assertEquals("dwezxdfgfgh", response.getBearerToken());
    }

    @Order(40)
    @Test
    public void accessDenied() throws Exception {
        final HandlerRequest<Model, StdCallbackContext> request = prepareRequest(
            Model.builder().repoName("repository").accessDenied(true).build());
        request.setAction(Action.READ);
        final Serializer serializer = new Serializer();
        final InputStream stream = prepareStream(serializer, request);
        final ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
        final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        final CredentialsProvider provider = prepareMockProvider();
        Context cxt = Mockito.mock(Context.class);
        // bail out immediately
        Mockito.when(cxt.getRemainingTimeInMillis()).thenReturn(50);
        Mockito.when(cxt.getLogger()).thenReturn(logger);

        ServiceHandlerWrapper wrapper = new ServiceHandlerWrapper(
            adapter,
            provider,
            Mockito.mock(MetricsPublisher.class),
            new CloudWatchScheduler(
                new CloudWatchEventsProvider(provider) {
                    @Override
                    public CloudWatchEventsClient get() {
                        return Mockito.mock(CloudWatchEventsClient.class);
                    }
                },
                logger),
            new Validator(),
            serializer
        );

        wrapper.handleRequest(stream, output, cxt);

        Response<Model> response = serializer.deserialize(output.toString(StandardCharsets.UTF_8.name()),
            new TypeReference<Response<Model>>(){});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(OperationStatus.FAILED, response.getOperationStatus());
        Assertions.assertEquals("dwezxdfgfgh", response.getBearerToken());
        Assertions.assertTrue(response.getMessage().contains("AccessDenied"));
    }

}
