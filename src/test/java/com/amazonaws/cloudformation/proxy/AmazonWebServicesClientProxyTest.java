package com.amazonaws.cloudformation.proxy;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.cloudformation.proxy.handler.Model;
import com.amazonaws.cloudformation.proxy.handler.ReadHandler;
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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmazonWebServicesClientProxyTest {

    @Test
    public void testInjectCredentialsAndInvoke() {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials, () -> 1000);

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
        Assertions.assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2() {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials, () -> 1000);

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
        Assertions.assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2Async() throws ExecutionException, InterruptedException {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials, () -> 1000);

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
        Assertions.assertThat(result.get()).isEqualTo(expectedResult);
    }

    private final Credentials MOCK =
       new Credentials("accessKeyId", "secretKey", "token");
    @Test
    public void badRequest() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LambdaLogger.class),
            MOCK,
            () -> (int)Duration.ofMinutes(2).toMillis() // just keep going
        );
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRespository", proxy.newProxy(() -> null), model, context)
            .request(m ->
                new CreateRequest.Builder().repoName(m.getRepoName()).build())
            .call((r, c) -> {
                throw new BadRequestException(mock(AwsServiceException.Builder.class));
            })
            .done(o -> ProgressEvent.success(model, context));
        assertEquals(OperationStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("BadRequest"));
    }

    @Test
    public void notFound() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LambdaLogger.class),
            MOCK,
            () -> (int)Duration.ofMinutes(2).toMillis() // just keep going
        );
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRespository", proxy.newProxy(() -> null), model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> {
                    throw new NotFoundException(mock(AwsServiceException.Builder.class));
                })
                .done(o -> ProgressEvent.success(model, context));
        assertEquals(OperationStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("NotFound"));
    }

    @Test
    public void accessDenied() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LambdaLogger.class),
            MOCK,
            () -> (int)Duration.ofMinutes(2).toMillis() // just keep going
        );
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRespository", proxy.newProxy(() -> null), model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> {
                    throw new AccessDenied(AwsServiceException.builder());
                })
                .done(o -> ProgressEvent.success(model, context));
        assertEquals(OperationStatus.FAILED, result.getStatus());
        assertTrue(result.getMessage().contains("AccessDenied"));
    }

    @Test
    public void throttleHandlingSuccess() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LambdaLogger.class),
            MOCK,
            () -> (int)Duration.ofMinutes(2).toMillis() // just keep going
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        int[] attempt = {2};
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = new ServiceClient();
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        CreateRequest[] requests = new CreateRequest[1];
        CreateResponse[] responses = new CreateResponse[1];
        DescribeRequest[] describeRequests = new DescribeRequest[1];
        DescribeResponse[] describeResponses = new DescribeResponse[1];
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    (requests[0] = new CreateRequest.Builder().repoName(m.getRepoName()).build()))
                .retry(new Delay.Fixed(3, 1, TimeUnit.SECONDS))
                .call((r, c) -> {
                    if (attempt[0]-- > 0) {
                        throw new ThrottleException(builder);
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
        assertEquals(OperationStatus.SUCCESS, result.getStatus());
        Model resultModel = result.getResourceModel();
        assertNotNull(resultModel.getArn());
        assertNotNull(resultModel.getCreated());

        Map<String, Object> callGraphs = context.getCallGraphs();
        assertTrue(callGraphs.containsKey("client:createRepository.request"));
        assertSame(requests[0], callGraphs.get("client:createRepository.request"));
        assertTrue(callGraphs.containsKey("client:createRepository.response"));
        assertSame(responses[0], callGraphs.get("client:createRepository.response"));
        assertTrue(callGraphs.containsKey("client:readRepository.request"));
        assertSame(describeRequests[0], callGraphs.get("client:readRepository.request"));
        assertTrue(callGraphs.containsKey("client:readRepository.response"));
        assertSame(describeResponses[0], callGraphs.get("client:readRepository.response"));
    }

    @Test
    public void throttedExceedRuntimeBailout() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LambdaLogger.class),
            MOCK,
            () -> (int)Duration.ofSeconds(1).toMillis() // signal we have only 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = new ServiceClient();
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .retry(new Delay.Fixed(5, 1, TimeUnit.SECONDS))
            .call((r, c) -> {
                throw new ThrottleException(AwsServiceException.builder());
            }).done(ign -> ProgressEvent.success(model, context));

        assertEquals(OperationStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    public void serviceCallWithStabilization() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            mock(LambdaLogger.class),
            MOCK,
            () -> (int)Duration.ofSeconds(1).toMillis() // signal we have only 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        int[] attempt = {2};
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = new ServiceClient();
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        ProgressEvent<Model, StdCallbackContext> result =
            proxy.initiate("client:createRepository", svcClient, model, context)
                .request(m ->
                    new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .retry(new Delay.Fixed(5, 1, TimeUnit.SECONDS))
                .call((r, c) ->
                    c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .stabilize((request, response, client1, model1, context1) -> attempt[0]-- > 0)
                .exceptFilter((request, exception, client1, model1, context1) ->
                    exception instanceof ThrottleException)
                .done(ign -> ProgressEvent.success(model, context));

        assertEquals(OperationStatus.SUCCESS, result.getStatus());
    }
}
