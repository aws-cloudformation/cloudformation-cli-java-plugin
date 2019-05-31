package com.amazonaws.cloudformation.proxy;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AmazonWebServicesRequestFunctionV2Async<RequestT extends AwsRequest, ResultT extends AwsResponse> {
    /**
     * Applies this function to the given arguments.
     *
     * @param request the function request argument
     * @return the function result
     */
    CompletableFuture<ResultT> apply(RequestT request);
}
