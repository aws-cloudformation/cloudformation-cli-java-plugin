package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class AmazonWebServicesClientProxyTest {

    @Test
    public void testInjectCredentialsAndInvoke() {

        final Credentials credentials = new Credentials("accessKeyId", "secretAccessKey", "sessionToken");

        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(credentials);

        final DescribeStackEventsRequest request = new DescribeStackEventsRequest();

        final DescribeStackEventsResult result = (DescribeStackEventsResult)proxy.injectCredentialsAndInvoke(
            AmazonCloudFormationClientBuilder.standard(),
            request,
            (AmazonWebServiceClient c, AmazonWebServiceRequest r) ->
                ((AmazonCloudFormation)c).describeStackEvents((DescribeStackEventsRequest) r));

        final DescribeStackEventsResult expectedResult = new DescribeStackEventsResult();

        assertThat(
            result,
            is(equalTo(expectedResult))
        );
    }

}
