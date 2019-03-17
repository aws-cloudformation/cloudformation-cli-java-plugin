package com.aws.cfn.proxy;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
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

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials);

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
        assertThat(
            result,
            is(equalTo(expectedResult))
        );
    }
}
