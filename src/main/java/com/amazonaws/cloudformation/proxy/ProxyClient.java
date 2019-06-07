package com.amazonaws.cloudformation.proxy;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ProxyClient<ClientT> {
    <RequestT extends AwsRequest, ResponseT extends AwsResponse>
        ResponseT injectCredentialsAndInvokeV2(
        final RequestT request,
        final Function<RequestT, ResponseT> requestFunction);

    <RequestT extends AwsRequest, ResponseT extends AwsResponse>
    CompletableFuture<ResponseT> injectCredentialsAndInvokeV2Aync(
        final RequestT request,
        final Function<RequestT, CompletableFuture<ResponseT>> requestFunction);

    ClientT client();
}
