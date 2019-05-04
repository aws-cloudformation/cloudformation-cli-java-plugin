package com.aws.cfn.proxy;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

@FunctionalInterface
public interface AmazonWebServicesRequestFunctionV2<RequestT extends AwsRequest, ResultT extends AwsResponse> {
    /**
     * Applies this function to the given arguments.
     *
     * @param request the function request argument
     * @return the function result
     */
    ResultT apply(RequestT request);
}
