package com.amazonaws.cloudformation.proxy;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.cloudformation.proxy.handler.Model;
import com.amazonaws.cloudformation.proxy.service.AccessDenied;
import com.amazonaws.cloudformation.proxy.service.BadRequestException;
import com.amazonaws.cloudformation.proxy.service.CreateRequest;
import com.amazonaws.cloudformation.proxy.service.CreateResponse;
import com.amazonaws.cloudformation.proxy.service.DescribeRequest;
import com.amazonaws.cloudformation.proxy.service.DescribeResponse;
import com.amazonaws.cloudformation.proxy.service.NotFoundException;
import com.amazonaws.cloudformation.proxy.service.ServiceClient;
import com.amazonaws.cloudformation.proxy.service.ThrottleException;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import org.joda.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmazonWebServicesClientProxyTest {
    //
    // The same that is asserted inside the ServiceClient
    //
    private final AwsSessionCredentials MockCreds = AwsSessionCredentials.create("accessKeyId", "secretKey", "token");

    @Test
    public void testInjectCredentialsAndInvoke() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final DescribeStackEventsRequest request = mock(DescribeStackEventsRequest.class);

        final DescribeStackEventsResult expectedResult = new DescribeStackEventsResult();
        expectedResult.setStackEvents(Collections.emptyList());

        final AmazonCloudFormation client = mock(AmazonCloudFormation.class);
        when(client.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(expectedResult);

        final DescribeStackEventsResult result = proxy.injectCredentialsAndInvoke(
            request,
            client::describeStackEvents);

        // ensure credentials are injected and then removed
        verify(request).setRequestCredentialsProvider(
                any(AWSStaticCredentialsProvider.class));
        verify(request).setRequestCredentialsProvider(
                eq(null));

        // ensure the return type matches
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest wrappedRequest =
            mock(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder builder =
            mock(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class)))
            .thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest request =
            mock(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final DescribeStackEventsResponse expectedResult = DescribeStackEventsResponse.builder()
            .stackEvents(Collections.emptyList())
            .build();

        final CloudFormationClient client = mock(CloudFormationClient.class);
        when(client.describeStackEvents(any(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class)))
            .thenReturn(expectedResult);

        final DescribeStackEventsResponse result = proxy.injectCredentialsAndInvokeV2(
            request,
            client::describeStackEvents);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).describeStackEvents(wrappedRequest);

        // ensure the return type matches
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2Async() throws ExecutionException, InterruptedException {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest wrappedRequest =
            mock(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder builder =
            mock(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class)))
            .thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest request =
            mock(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final DescribeStackEventsResponse expectedResult = DescribeStackEventsResponse.builder()
            .stackEvents(Collections.emptyList())
            .build();

        final CloudFormationAsyncClient client = mock(CloudFormationAsyncClient.class);
        when(client.describeStackEvents(any(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));

        final CompletableFuture<DescribeStackEventsResponse> result = proxy.injectCredentialsAndInvokeV2Async(
            request,
            client::describeStackEvents);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).describeStackEvents(wrappedRequest);

        // ensure the return type matches
        assertThat(result.get()).isEqualTo(expectedResult);
    }

    private final Credentials MOCK =
        new Credentials("accessKeyId", "secretKey", "token");

    @Test
    public void badRequest() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofMinutes(2).toMillis() // just keep going
        );
        final Model model = new Model();
        model.setRepoName("NewRepo");
        final StdCallbackContext context = new StdCallbackContext();
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(400);
        final ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRespository", proxy.newProxy(() -> null), model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> {
                    throw new BadRequestException(mock(AwsServiceException.Builder.class)) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public AwsErrorDetails awsErrorDetails() {
                            return
                                AwsErrorDetails.builder()
                                    .errorCode("BadRequest")
                                    .errorMessage("Bad Parameter in request")
                                    .sdkHttpResponse(sdkHttpResponse)
                                    .build();
                        }
                    };
                })
                .done(o -> ProgressEvent.success(model, context));
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getMessage()).contains("BadRequest");
    }

    @Test
    public void notFound() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofMinutes(2).toMillis() // just keep going
        );
        final Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(404);
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRespository", proxy.newProxy(() -> null), model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> {
                    throw new NotFoundException(mock(AwsServiceException.Builder.class)) {
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
                })
                .done(o -> ProgressEvent.success(model, context));
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getMessage()).contains("NotFound");
    }

    @Test
    public void accessDenied() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofMinutes(2).toMillis() // just keep going
        );
        final Model model = new Model();
        model.setRepoName("NewRepo");
        final StdCallbackContext context = new StdCallbackContext();
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(401);
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRespository", proxy.newProxy(() -> null), model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> {
                    throw new AccessDenied(AwsServiceException.builder()) {
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
                })
                .done(o -> ProgressEvent.success(model, context));
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getMessage()).contains("AccessDenied");
    }

    @Test
    public void throttleHandlingSuccess() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofMinutes(2).toMillis() // just keep going
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        int[] attempt = {2};
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        CreateRequest[] requests = new CreateRequest[1];
        CreateResponse[] responses = new CreateResponse[1];
        DescribeRequest[] describeRequests = new DescribeRequest[1];
        DescribeResponse[] describeResponses = new DescribeResponse[1];

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
        when(client.describeRepository(eq(describeRequest))).thenReturn(
            new DescribeResponse.Builder()
                .repoName(model.getRepoName())
                .repoArn("some-arn")
                .createdWhen(Instant.now().toDate())
                .build());

        final ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);

        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(429);

        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    (requests[0] = new CreateRequest.Builder().repoName(m.getRepoName()).build()))
                .retry(new Delay.Constant(1, 3, TimeUnit.SECONDS))
                .call((r, c) -> {
                    if (attempt[0]-- > 0) {
                        throw new ThrottleException(builder) {
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
                    }
                    return (responses[0] = c.injectCredentialsAndInvokeV2(r, c.client()::createRepository));
                })
                .done((request, response, client1, model1, context1) ->
                    proxy.initiate("client:readRepository", client1, model1, context1)
                        .request(m -> (describeRequests[0] = new DescribeRequest.Builder().repoName(m.getRepoName()).build()))
                        .call((r, c) ->
                            (describeResponses[0] = c.injectCredentialsAndInvokeV2(r, c.client()::describeRepository)))
                        .done(r -> {
                            Model resultModel = new Model();
                            resultModel.setRepoName(r.getRepoName());
                            resultModel.setArn(r.getRepoArn());
                            resultModel.setCreated(r.getCreatedWhen());
                            return ProgressEvent.success(resultModel, context);
                        }));

        verify(client).createRepository(eq(createRequest));
        verify(client).describeRepository(eq(describeRequest));

        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        final Model resultModel = result.getResourceModel();
        assertThat(resultModel.getArn()).isNotNull();
        assertThat(resultModel.getCreated()).isNotNull();

        Map<String, Object> callGraphs = context.getCallGraphs();
        assertThat(callGraphs.containsKey("client:createRepository.request")).isEqualTo(true);
        assertSame(requests[0], callGraphs.get("client:createRepository.request"));
        assertThat(callGraphs.containsKey("client:createRepository.response")).isEqualTo(true);
        assertSame(responses[0], callGraphs.get("client:createRepository.response"));
        assertThat(callGraphs.containsKey("client:readRepository.request")).isEqualTo(true);
        assertSame(describeRequests[0], callGraphs.get("client:readRepository.request"));
        assertThat(callGraphs.containsKey("client:readRepository.response")).isEqualTo(true);
        assertSame(describeResponses[0], callGraphs.get("client:readRepository.response"));
    }

    @Test
    public void throttedExceedRuntimeBailout() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofSeconds(1).toMillis() // signal we have only 1s left.
        );
        final AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        final Model model = new Model();
        model.setRepoName("NewRepo");
        final StdCallbackContext context = new StdCallbackContext();
        final ServiceClient client = mock(ServiceClient.class);
        final ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(429);

        final ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .retry(new Delay.Constant(5, 10, TimeUnit.SECONDS))
                .call((r, c) -> {
                    throw new ThrottleException(AwsServiceException.builder()) {
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
                }).done(ign -> ProgressEvent.success(model, context));

        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
    }

    @Test
    public void serviceCallWithStabilization() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofSeconds(1).toMillis() // signal we have only 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        int[] attempt = {2};
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = mock(ServiceClient.class);
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .retry(new Delay.Constant(5, 1, TimeUnit.SECONDS))
                .call((r, c) ->
                    c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .stabilize((request, response, client1, model1, context1) -> attempt[0]-- > 0)
                .exceptFilter((request, exception, client1, model1, context1) ->
                    exception instanceof ThrottleException)
                .done(ign -> ProgressEvent.success(model, context));

        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void throwNotRetryableException() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofSeconds(1).toMillis() // signal we have only 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = mock(ServiceClient.class);
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, g) -> {
                    NonRetryableException e = NonRetryableException.builder().build();
                    throw e;
                }).success();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void throwOtherException() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LoggerProxy.class),
            MOCK,
            () -> Duration.ofSeconds(1).toMillis() // signal we have only 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = mock(ServiceClient.class);
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, g) -> {
                    throw new RuntimeException("Fail");
                }).success();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);

    }
}
