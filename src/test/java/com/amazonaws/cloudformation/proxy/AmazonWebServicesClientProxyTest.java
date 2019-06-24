/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the &quot;License&quot;).
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the &quot;license&quot; file accompanying this file. This file is distributed
* on an &quot;AS IS&quot; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;

public class AmazonWebServicesClientProxyTest {

    @Test
    public void testInjectCredentialsAndInvoke() {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials);

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
    public void testInjectCredentialsAndInvokeV2() {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials);

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

        // verify the wrapped request is sent over the client
        verify(client).describeStackEvents(wrappedRequest);

        // ensure the return type matches
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void testInjectCredentialsAndInvokeV2Async() throws ExecutionException, InterruptedException {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials);

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

        // verify the wrapped request is sent over the client
        verify(client).describeStackEvents(wrappedRequest);

        // ensure the return type matches
        assertThat(result.get()).isEqualTo(expectedResult);
    }
}
