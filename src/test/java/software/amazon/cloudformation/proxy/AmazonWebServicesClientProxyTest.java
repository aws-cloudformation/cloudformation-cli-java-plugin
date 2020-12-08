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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.joda.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.delay.Constant;
import software.amazon.cloudformation.proxy.handler.Model;
import software.amazon.cloudformation.proxy.service.AccessDenied;
import software.amazon.cloudformation.proxy.service.BadRequestException;
import software.amazon.cloudformation.proxy.service.CreateRequest;
import software.amazon.cloudformation.proxy.service.CreateResponse;
import software.amazon.cloudformation.proxy.service.DescribeRequest;
import software.amazon.cloudformation.proxy.service.DescribeResponse;
import software.amazon.cloudformation.proxy.service.NotFoundException;
import software.amazon.cloudformation.proxy.service.ServiceClient;
import software.amazon.cloudformation.proxy.service.ThrottleException;

public class AmazonWebServicesClientProxyTest {
    //
    // The same that is asserted inside the ServiceClient
    //
    private final AwsSessionCredentials MockCreds = AwsSessionCredentials.create("accessKeyId", "secretKey", "token");

    //
    // Empty method for testing injectCredentialsAndInvoke with void returns
    //
    public static void dummyMethod(DescribeStackEventsRequest request) {
    }

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

        final DescribeStackEventsResult result = proxy.injectCredentialsAndInvoke(request, client::describeStackEvents);

        // ensure credentials are injected and then removed
        verify(request).setRequestCredentialsProvider(any(AWSStaticCredentialsProvider.class));
        verify(request).setRequestCredentialsProvider(eq(null));

        // ensure the return type matches
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void testInjectCredentialsAndInvokeWithVoidFunction() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final DescribeStackEventsRequest request = mock(DescribeStackEventsRequest.class);

        proxy.injectCredentialsAndInvoke(request, AmazonWebServicesClientProxyTest::dummyMethod);

        // ensure credentials are injected and then removed
        verify(request).setRequestCredentialsProvider(any(AWSStaticCredentialsProvider.class));
        verify(request).setRequestCredentialsProvider(eq(null));
    }

    @Test
    public void testInjectCredentialsAndInvokeWithError() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final DescribeStackEventsRequest request = mock(DescribeStackEventsRequest.class);

        final AmazonCloudFormation client = mock(AmazonCloudFormation.class);
        when(client.describeStackEvents(any(DescribeStackEventsRequest.class))).thenThrow(new RuntimeException("Sorry"));

        final RuntimeException expectedException = assertThrows(RuntimeException.class,
            () -> proxy.injectCredentialsAndInvoke(request, client::describeStackEvents), "Expected Runtime Exception.");
        assertEquals(expectedException.getMessage(), "Sorry");

        // ensure credentials are injected and then removed
        verify(request).setRequestCredentialsProvider(any(AWSStaticCredentialsProvider.class));
        verify(request).setRequestCredentialsProvider(eq(null));
    }

    @Test
    public void testInjectCredentialsAndInvokeV2() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest wrappedRequest = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder builder = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest request = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final DescribeStackEventsResponse expectedResult = DescribeStackEventsResponse.builder()
            .stackEvents(Collections.emptyList()).build();

        final CloudFormationClient client = mock(CloudFormationClient.class);
        when(client
            .describeStackEvents(any(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class)))
                .thenReturn(expectedResult);

        final DescribeStackEventsResponse result = proxy.injectCredentialsAndInvokeV2(request, client::describeStackEvents);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).describeStackEvents(wrappedRequest);

        // ensure the return type matches
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public <ResultT extends AwsResponse, IterableT extends SdkIterable<ResultT>> void testInjectCredentialsAndInvokeV2Iterable() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");
        final ListObjectsV2Iterable response = mock(ListObjectsV2Iterable.class);

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.s3.model.ListObjectsV2Request wrappedRequest = mock(
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request.class);

        final software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder builder = mock(
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.s3.model.ListObjectsV2Request request = mock(
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request.class);
        when(request.toBuilder()).thenReturn(builder);

        final S3Client client = mock(S3Client.class);

        when(client.listObjectsV2Paginator(any(software.amazon.awssdk.services.s3.model.ListObjectsV2Request.class)))
            .thenReturn(response);

        final ListObjectsV2Iterable result = proxy.injectCredentialsAndInvokeIterableV2(request, client::listObjectsV2Paginator);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).listObjectsV2Paginator(wrappedRequest);

        // ensure the return type matches
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2Async() throws ExecutionException, InterruptedException {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest wrappedRequest = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder builder = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest request = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final DescribeStackEventsResponse expectedResult = DescribeStackEventsResponse.builder()
            .stackEvents(Collections.emptyList()).build();

        final CloudFormationAsyncClient client = mock(CloudFormationAsyncClient.class);
        when(client
            .describeStackEvents(any(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(expectedResult));

        final CompletableFuture<
            DescribeStackEventsResponse> result = proxy.injectCredentialsAndInvokeV2Async(request, client::describeStackEvents);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).describeStackEvents(wrappedRequest);

        // ensure the return type matches
        assertThat(result.get()).isEqualTo(expectedResult);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2Async_WithException() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest wrappedRequest = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);

        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder builder = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest request = mock(
            software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final CloudFormationAsyncClient client = mock(CloudFormationAsyncClient.class);

        when(client
            .describeStackEvents(any(software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest.class)))
                .thenThrow(new TerminalException(new RuntimeException("Sorry")));
        assertThrows(RuntimeException.class, () -> proxy.injectCredentialsAndInvokeV2Async(request, client::describeStackEvents),
            "Expected Runtime Exception.");

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).describeStackEvents(wrappedRequest);

    }

    @Test
    public void testInjectCredentialsAndInvokeV2InputStream() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");
        final ResponseInputStream<?> responseInputStream = mock(ResponseInputStream.class);

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest wrappedRequest = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder builder = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.s3.model.GetObjectRequest request = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final S3Client client = mock(S3Client.class);

        doReturn(responseInputStream).when(client)
            .getObject(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class));

        final ResponseInputStream<
            GetObjectResponse> result = proxy.injectCredentialsAndInvokeV2InputStream(request, client::getObject);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).getObject(wrappedRequest);

        // ensure the return type matches
        assertThat(result).isEqualTo(responseInputStream);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2InputStream_Exception() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest wrappedRequest = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder builder = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.s3.model.GetObjectRequest request = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final S3Client client = mock(S3Client.class);

        doThrow(new TerminalException(new RuntimeException("Sorry"))).when(client)
            .getObject(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class));

        assertThrows(RuntimeException.class, () -> proxy.injectCredentialsAndInvokeV2InputStream(request, client::getObject),
            "Expected Runtime Exception.");

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).getObject(wrappedRequest);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2Bytes() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");
        final ResponseBytes<?> responseBytes = mock(ResponseBytes.class);

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest wrappedRequest = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder builder = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.s3.model.GetObjectRequest request = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final S3Client client = mock(S3Client.class);

        doReturn(responseBytes).when(client)
            .getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class));

        final ResponseBytes<
            GetObjectResponse> result = proxy.injectCredentialsAndInvokeV2Bytes(request, client::getObjectAsBytes);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).getObjectAsBytes(wrappedRequest);

        // ensure the return type matches
        assertThat(result).isEqualTo(responseBytes);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2Bytes_Exception() {

        final LoggerProxy loggerProxy = mock(LoggerProxy.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(loggerProxy, credentials, () -> 1000L);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest wrappedRequest = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);

        final software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder builder = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class))).thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final software.amazon.awssdk.services.s3.model.GetObjectRequest request = mock(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final S3Client client = mock(S3Client.class);

        doThrow(new TerminalException(new RuntimeException("Sorry"))).when(client)
            .getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class));

        assertThrows(RuntimeException.class, () -> proxy.injectCredentialsAndInvokeV2Bytes(request, client::getObjectAsBytes),
            "Expected Runtime Exception.");

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the initiate
        verify(client).getObjectAsBytes(wrappedRequest);
    }

    private final Credentials MOCK = new Credentials("accessKeyId", "secretKey", "token");

    @Test
    public void badRequest() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                                    () -> Duration.ofMinutes(2).toMillis() // just
                                                                                                                           // keep
                                                                                                                           // going

        );
        final Model model = new Model();
        model.setRepoName("NewRepo");
        final StdCallbackContext context = new StdCallbackContext();
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(400);
        final ProgressEvent<Model,
            StdCallbackContext> result = proxy
                .initiate("client:createRespository", proxy.newProxy(() -> mock(ServiceClient.class)), model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .makeServiceCall((r, c) -> {
                    throw new BadRequestException(new AwsServiceException(AwsServiceException.builder()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public AwsErrorDetails awsErrorDetails() {
                            return AwsErrorDetails.builder().errorCode("BadRequest").errorMessage("Bad Parameter in request")
                                .sdkHttpResponse(sdkHttpResponse).build();
                        }
                    }.toBuilder());
                }).done(o -> ProgressEvent.success(model, context));
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getMessage()).contains("Bad Parameter");
    }

    @Test
    public void notFound() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                                    () -> Duration.ofMinutes(2).toMillis() // just
                                                                                                                           // keep
                                                                                                                           // going
        );
        final Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(404);
        ProgressEvent<Model,
            StdCallbackContext> result = proxy
                .initiate("client:createRespository", proxy.newProxy(() -> mock(ServiceClient.class)), model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .makeServiceCall((r, c) -> {
                    throw new NotFoundException(new AwsServiceException(AwsServiceException.builder()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public AwsErrorDetails awsErrorDetails() {
                            return AwsErrorDetails.builder().errorCode("NotFound").errorMessage("Repo not existing")
                                .sdkHttpResponse(sdkHttpResponse).build();
                        }
                    }.toBuilder());
                }).done(o -> ProgressEvent.success(model, context));
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getMessage()).contains("Repo not existing");
    }

    @Test
    public void accessDenied() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                                    () -> Duration.ofMinutes(2).toMillis() // just
                                                                                                                           // keep
                                                                                                                           // going
        );
        final Model model = new Model();
        model.setRepoName("NewRepo");
        final StdCallbackContext context = new StdCallbackContext();
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(401);
        ProgressEvent<Model,
            StdCallbackContext> result = proxy
                .initiate("client:createRespository", proxy.newProxy(() -> mock(ServiceClient.class)), model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .makeServiceCall((r, c) -> {
                    throw new AccessDenied(new AwsServiceException(AwsServiceException.builder()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public AwsErrorDetails awsErrorDetails() {
                            return AwsErrorDetails.builder().errorCode("AccessDenied: 401").errorMessage("Token Invalid")
                                .sdkHttpResponse(sdkHttpResponse).build();
                        }
                    }.toBuilder());
                }).done(o -> ProgressEvent.success(model, context));
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getMessage()).contains("Token Invalid");
    }

    @Test
    public void throttleHandlingSuccess() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofMinutes(2).toMillis() // just keep
                                                                                                                     // going
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        int[] attempt = { 2 };
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        CreateRequest[] requests = new CreateRequest[1];
        CreateResponse[] responses = new CreateResponse[1];
        DescribeRequest[] describeRequests = new DescribeRequest[1];
        DescribeResponse[] describeResponses = new DescribeResponse[1];

        final ServiceClient client = mock(ServiceClient.class);
        final CreateRequest createRequest = new CreateRequest.Builder().repoName(model.getRepoName()).overrideConfiguration(
            AwsRequestOverrideConfiguration.builder().credentialsProvider(StaticCredentialsProvider.create(MockCreds)).build())
            .build();

        when(client.createRepository(eq(createRequest)))
            .thenReturn(new CreateResponse.Builder().repoName(model.getRepoName()).build());
        final DescribeRequest describeRequest = new DescribeRequest.Builder().repoName(model.getRepoName()).overrideConfiguration(
            AwsRequestOverrideConfiguration.builder().credentialsProvider(StaticCredentialsProvider.create(MockCreds)).build())
            .build();
        when(client.describeRepository(eq(describeRequest))).thenReturn(new DescribeResponse.Builder()
            .repoName(model.getRepoName()).repoArn("some-arn").createdWhen(Instant.now().toDate()).build());

        final ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);

        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(429);

        ProgressEvent<Model,
            StdCallbackContext> result = proxy.initiate("client:createRepository", svcClient, model, context)
                .translateToServiceRequest(m -> (requests[0] = new CreateRequest.Builder().repoName(m.getRepoName()).build()))
                .backoffDelay(Constant.of().delay(Duration.ofSeconds(1)).timeout(Duration.ofSeconds(3)).build())
                .makeServiceCall((r, c) -> {
                    if (attempt[0]-- > 0) {
                        throw new ThrottleException(builder) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public AwsErrorDetails awsErrorDetails() {
                                return AwsErrorDetails.builder().errorCode("ThrottleException")
                                    .errorMessage("Temporary Limit Exceeded").sdkHttpResponse(sdkHttpResponse).build();
                            }
                        };
                    }
                    return (responses[0] = c.injectCredentialsAndInvokeV2(r, c.client()::createRepository));
                })
                .done((request, response, client1, model1, context1) -> proxy
                    .initiate("client:readRepository", client1, model1, context1)
                    .translateToServiceRequest(
                        m -> (describeRequests[0] = new DescribeRequest.Builder().repoName(m.getRepoName()).build()))
                    .makeServiceCall(
                        (r, c) -> (describeResponses[0] = c.injectCredentialsAndInvokeV2(r, c.client()::describeRepository)))
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

        Object objToCmp = context.findFirstRequestByContains("client:createRepository");
        assertThat(objToCmp).isNotNull();
        assertThat(requests[0]).isSameAs(objToCmp);

        objToCmp = context.findFirstResponseByContains("client:createRepository");
        assertThat(objToCmp).isNotNull();
        assertThat(responses[0]).isSameAs(objToCmp);

        objToCmp = context.findFirstRequestByContains("client:readRepository");
        assertThat(objToCmp).isNotNull();
        assertThat(describeRequests[0]).isSameAs(objToCmp);

        objToCmp = context.findFirstResponseByContains("client:readRepository");
        assertThat(objToCmp).isNotNull();
        assertThat(describeResponses[0]).isSameAs(objToCmp);
    }

    @Test
    public void throttledExceedRuntimeBailout() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                                    () -> Duration.ofSeconds(1).toMillis() // signal
                                                                                                                           // we
                                                                                                                           // have
                                                                                                                           // only
                                                                                                                           // 1s
                                                                                                                           // left.
        );
        final AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        final Model model = new Model();
        model.setRepoName("NewRepo");
        final StdCallbackContext context = new StdCallbackContext();
        final ServiceClient client = mock(ServiceClient.class);
        final ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(429);

        final ProgressEvent<Model,
            StdCallbackContext> result = proxy.initiate("client:createRepository", svcClient, model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .backoffDelay(Constant.of().delay(Duration.ofSeconds(5)).timeout(Duration.ofSeconds(10)).build())
                .makeServiceCall((r, c) -> {
                    throw new ThrottleException(AwsServiceException.builder()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public AwsErrorDetails awsErrorDetails() {
                            return AwsErrorDetails.builder().errorCode("ThrottleException")
                                .errorMessage("Temporary Limit Exceeded").sdkHttpResponse(sdkHttpResponse).build();
                        }
                    };
                }).done(ign -> ProgressEvent.success(model, context));
        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(result.getCallbackDelaySeconds()).isGreaterThan(0);
    }

    @Test
    public void serviceCallWithStabilization() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis() // signal we
                                                                                                                     // have only
                                                                                                                     // 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        int[] attempt = { 2 };
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = mock(ServiceClient.class);
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        ProgressEvent<Model,
            StdCallbackContext> result = proxy.initiate("client:createRepository", svcClient, model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .backoffDelay(Constant.of().delay(Duration.ofSeconds(5)).timeout(Duration.ofSeconds(15)).build())
                .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .stabilize((request, response, client1, model1, context1) -> attempt[0]-- > 0)
                .retryErrorFilter((request, exception, client1, model1, context1) -> exception instanceof ThrottleException)
                .done(ign -> ProgressEvent.success(model, context));

        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void serviceCallWithFilterException() {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                                    () -> Duration.ofSeconds(1).toMillis() // signal
                                                                                                                           // we
                                                                                                                           // have
                                                                                                                           // only
                                                                                                                           // 1
                                                                                                                           // second
        );
        final Model model = new Model();
        model.setRepoName("NewRepo");
        final StdCallbackContext context = new StdCallbackContext();
        final ServiceClient client = mock(ServiceClient.class);
        final ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        final SdkHttpResponse sdkHttpResponse = mock(SdkHttpResponse.class);
        when(sdkHttpResponse.statusCode()).thenReturn(503);
        when(client.createRepository(any(CreateRequest.class))).thenThrow(new ThrottleException(AwsServiceException.builder()) {
            private static final long serialVersionUID = 1L;

            @Override
            public AwsErrorDetails awsErrorDetails() {
                return AwsErrorDetails.builder().errorCode("ThrottleException").errorMessage("Temporary Limit Exceeded")
                    .sdkHttpResponse(sdkHttpResponse).build();
            }
        });

        final ProgressEvent<Model,
            StdCallbackContext> result = proxy.initiate("client:createRepository", svcClient, model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .backoffDelay(Constant.of().delay(Duration.ofSeconds(5)).timeout(Duration.ofSeconds(10)).build())
                .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .retryErrorFilter((request, response, client1, model1, context1) -> response instanceof ThrottleException)
                .done(ign -> ProgressEvent.success(model, context));

        assertThat(result.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);

    }

    @Test
    public void throwNotRetryableException() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis() // signal we
                                                                                                                     // have only
                                                                                                                     // 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = mock(ServiceClient.class);
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        ProgressEvent<Model,
            StdCallbackContext> result = proxy.initiate("client:createRepository", svcClient, model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .makeServiceCall((r, g) -> {
                    NonRetryableException e = NonRetryableException.builder().build();
                    throw e;
                }).success();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void throwOtherException() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis() // signal we
                                                                                                                     // have only
                                                                                                                     // 1s left.
        );
        AwsServiceException.Builder builder = mock(AwsServiceException.Builder.class);
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = mock(ServiceClient.class);
        ProxyClient<ServiceClient> svcClient = proxy.newProxy(() -> client);
        assertThrows(ResourceAlreadyExistsException.class,
            () -> proxy.initiate("client:createRepository", svcClient, model, context)
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .makeServiceCall((r, g) -> {
                    throw new ResourceAlreadyExistsException(new RuntimeException("Fail"));
                }).success());
    }

    @Test
    public void useInitiatorPattern() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis()); // signal
                                                                                                                       // we
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ServiceClient client = mock(ServiceClient.class);
        final CallChain.Initiator<ServiceClient, Model, StdCallbackContext> invoker = proxy
            .newInitiator(() -> client, model, context).rebindModel(model).rebindCallback(context);
        assertThrows(ResourceAlreadyExistsException.class,
            () -> invoker.initiate("client:createRepository")
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .makeServiceCall((r, g) -> {
                    throw new ResourceAlreadyExistsException(new RuntimeException("Fail"));
                }).success());
    }

    @Test
    public void thenChainPattern() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis());
        Model model = new Model();
        model.setRepoName("NewRepo");
        StdCallbackContext context = new StdCallbackContext();
        ProgressEvent.progress(model, context).then(event -> ProgressEvent.success(model, context));

        ProgressEvent<Model,
            StdCallbackContext> event = ProgressEvent.progress(model, context).then(
                event_ -> ProgressEvent.defaultFailureHandler(new RuntimeException("failed"), HandlerErrorCode.InternalFailure))
                .then(event_ -> {
                    fail("Did not reach the chain here");
                    return ProgressEvent.success(model, context);
                });
        assertThat(event.isFailed()).isTrue();
    }

    @Test
    public void automaticNamedRequests() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis());
        final String repoName = "NewRepo";
        final Model model = new Model();
        model.setRepoName(repoName);
        final StdCallbackContext context = new StdCallbackContext();
        //
        // Mock calls
        //
        final ServiceClient client = mock(ServiceClient.class);
        when(client.createRepository(any(CreateRequest.class)))
            .thenReturn(new CreateResponse.Builder().repoName(model.getRepoName()).build());
        when(client.serviceName()).thenReturn("repositoryService");

        CallChain.Initiator<ServiceClient, Model,
            StdCallbackContext> initiator = proxy.newInitiator(() -> client, model, context);

        final CreateRequest createRepository = new CreateRequest.Builder().repoName(repoName).build();
        ProgressEvent<Model, StdCallbackContext> result = initiator.translateToServiceRequest(m -> createRepository)
            .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository)).success();

        ProgressEvent<Model, StdCallbackContext> result_2 = initiator.translateToServiceRequest(m -> createRepository)
            .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository)).success();

        assertThat(result).isNotSameAs(result_2);
        assertThat(result_2).isEqualTo(result);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        CreateRequest internal = context.findFirstRequestByContains("repositoryService:Create");
        assertThat(internal).isNotNull();
        assertThat(internal).isSameAs(createRepository);

        Map<String, Object> callGraphs = context.callGraphs();
        assertThat(callGraphs.size()).isEqualTo(3);
        // verify this was called only once for both requests.
        verify(client).createRepository(any(CreateRequest.class));
    }

    @Test
    public void automaticNamedUniqueRequests() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis());
        final String repoName = "NewRepo";
        final Model model = new Model();
        model.setRepoName(repoName);
        final StdCallbackContext context = new StdCallbackContext();
        //
        // TODO add the mocks needed
        //
        final ServiceClient client = mock(ServiceClient.class);
        when(client.createRepository(any(CreateRequest.class)))
            .thenAnswer(invocation -> new CreateResponse.Builder().repoName(model.getRepoName()).build());
        when(client.serviceName()).thenReturn("repositoryService");

        final CallChain.Initiator<ServiceClient, Model,
            StdCallbackContext> initiator = proxy.newInitiator(() -> client, model, context);

        final CreateRequest createRepository = new CreateRequest.Builder().repoName(repoName).build();
        ProgressEvent<Model, StdCallbackContext> result = initiator.translateToServiceRequest(m -> createRepository)
            .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository)).success();

        model.setRepoName(repoName + "-2");
        ProgressEvent<Model,
            StdCallbackContext> result_2 = initiator.rebindModel(Model.builder().repoName(repoName + "-2").build())
                .translateToServiceRequest(m -> new CreateRequest.Builder().repoName(model.getRepoName()).build())
                .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository)).success();
        model.setRepoName(repoName);

        assertThat(result).isNotEqualTo(result_2);
        CreateRequest internal = context.findFirstRequestByContains("repositoryService:Create");
        assertThat(internal).isNotNull();
        assertThat(internal).isSameAs(createRepository); // we picked the one with the first call

        List<CreateResponse> responses = context.findAllResponseByContains("repositoryService:Create");
        assertThat(responses.size()).isEqualTo(2);
        List<CreateResponse> expected = Arrays.asList(new CreateResponse.Builder().repoName(repoName).build(),
            new CreateResponse.Builder().repoName(repoName + "-2").build());
        assertThat(responses).isEqualTo(expected);

        verify(client, times(2)).createRepository(any(CreateRequest.class));
    }

    @Test
    public void nullRequestTest() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(mock(LoggerProxy.class), MOCK,
                                                                              () -> Duration.ofSeconds(1).toMillis());
        final String repoName = "NewRepo";
        final Model model = new Model();
        model.setRepoName(repoName);
        final StdCallbackContext context = new StdCallbackContext();
        //
        // Mock calls
        //
        final ServiceClient client = mock(ServiceClient.class);
        final CallChain.Initiator<ServiceClient, Model,
            StdCallbackContext> initiator = proxy.newInitiator(() -> client, model, context);
        ProgressEvent<Model, StdCallbackContext> result = initiator.translateToServiceRequest(m -> (CreateRequest) null)
            .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository)).success();

        assertThat(result).isNotNull();

    }
}
