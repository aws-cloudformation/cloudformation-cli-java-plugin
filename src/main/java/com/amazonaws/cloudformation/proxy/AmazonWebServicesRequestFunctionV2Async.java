package com.amazonaws.cloudformation.proxy;

import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

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
