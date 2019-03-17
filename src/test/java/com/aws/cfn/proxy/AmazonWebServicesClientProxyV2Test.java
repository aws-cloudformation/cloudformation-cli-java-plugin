package com.aws.cfn.proxy;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AmazonWebServicesClientProxyV2Test {

    @Test
    public void testInjectCredentialsAndInvokeV2() throws ExecutionException, InterruptedException {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxyV2 proxy = new AmazonWebServicesClientProxyV2(lambdaLogger, credentials);

        final DescribeStackEventsRequest wrappedRequest = mock(DescribeStackEventsRequest.class);

        final DescribeStackEventsRequest.Builder builder = mock(DescribeStackEventsRequest.Builder.class);
        when(builder.overrideConfiguration(any(AwsRequestOverrideConfiguration.class)))
            .thenReturn(builder);
        when(builder.build()).thenReturn(wrappedRequest);
        final DescribeStackEventsRequest request = mock(DescribeStackEventsRequest.class);
        when(request.toBuilder()).thenReturn(builder);

        final DescribeStackEventsResponse expectedResult = DescribeStackEventsResponse.builder()
            .stackEvents(Collections.EMPTY_LIST)
            .build();

        final CloudFormationAsyncClient client = mock(CloudFormationAsyncClient.class);
        when(client.describeStackEvents(any(DescribeStackEventsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(expectedResult));

        final CompletableFuture<DescribeStackEventsResponse> result = proxy.injectCredentialsAndInvoke(
            request,
            client::describeStackEvents);

        // verify request is rebuilt for injection
        verify(request).toBuilder();

        // verify the wrapped request is sent over the client
        verify(client).describeStackEvents(wrappedRequest);

        // ensure the return type matches
        assertThat(
            result.get(),
            is(equalTo(expectedResult))
        );
    }
}
