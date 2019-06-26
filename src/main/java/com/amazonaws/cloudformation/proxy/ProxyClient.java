package com.amazonaws.cloudformation.proxy;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This class provides a wrapper for the client and provides methods to inject
 * scoped credentials for each request context when invoking AWS services. This
 * is the primary mechanism for handlers to invoke service requests to ensure the
 * right scoped credentials are being injected.
 * IMPORTANT: <em>DO NOT DIRECTLY INVOKE methods</em> on the client. Use the client
 * to provide method references to be invoked. See example below
 *
 * {@code
 *    CreateStreamResponse call(ProxyClient<KinesisClient> client, CreateStreamRequest request) {
 *        return client.injectCredentialsAndInvokeV2(
 *            request, client.client()::createStream); // method reference
 *    }
 * }
 * @param <ClientT> the AWS client like KinesisClient that is used to invoke
 *                  the web service
 */
public interface ProxyClient<ClientT> {
    /**
     * This is the synchronous version of making API calls.
     * @param request, the AWS service request that we need to make
     * @param requestFunction, this is a Lambda closure that provide the actual API
     *                         that needs to the invoked.
     * @param <RequestT> the request type
     * @param <ResponseT> the response from the request
     * @return the response if successful. Else it will propagate all
     *         {@link software.amazon.awssdk.awscore.exception.AwsServiceException} that
     *         is thrown or {@link software.amazon.awssdk.core.exception.SdkClientException}
     *         if there is client side problem
     *
     */
    <RequestT extends AwsRequest, ResponseT extends AwsResponse>
        ResponseT injectCredentialsAndInvokeV2(
        final RequestT request,
        final Function<RequestT, ResponseT> requestFunction);

    /**
     * This is the asynchronous version of making API calls.
     * @param request, the AWS service request that we need to make
     * @param requestFunction, this is a Lambda closure that provide the actual API
     *                         that needs to the invoked.
     * @param <RequestT> the request type
     * @param <ResponseT> the response from the request
     * @return the response if successful. Else it will propagate all
     *         {@link software.amazon.awssdk.awscore.exception.AwsServiceException} that
     *         is thrown or {@link software.amazon.awssdk.core.exception.SdkClientException}
     *         if there is client side problem
     */
    <RequestT extends AwsRequest, ResponseT extends AwsResponse>
        CompletableFuture<ResponseT> injectCredentialsAndInvokeV2Aync(
        final RequestT request,
        final Function<RequestT, CompletableFuture<ResponseT>> requestFunction);

    /**
     * @return the actual AWS service client that we need to use to provide the actual method
     *         we are going to call.
     */
    ClientT client();
}
