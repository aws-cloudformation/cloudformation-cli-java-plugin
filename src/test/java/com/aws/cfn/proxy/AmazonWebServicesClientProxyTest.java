package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

public class AmazonWebServicesClientProxyTest {

    @Test
    public void testInjectCredentialsAndInvoke() {

        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);
        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(lambdaLogger, credentials);

        final DescribeStackEventsRequest request = new DescribeStackEventsRequest();

        final AmazonCloudFormation client = AmazonCloudFormationClientBuilder.standard().build();

        final DescribeStackEventsResult result = proxy.injectCredentialsAndInvoke(
            request,
            client::describeStackEvents);

        final DescribeStackEventsResult expectedResult = new DescribeStackEventsResult();

        assertThat(
            result,
            is(equalTo(expectedResult))
        );
    }

}
