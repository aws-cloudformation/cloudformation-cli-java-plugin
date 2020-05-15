/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.proxy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;

/**
 * This class provides a wrapper for the client and provides methods to inject
 * scoped credentials for each request context when invoking AWS services. This
 * is the primary mechanism for handlers to invoke service requests to ensure
 * the right scoped credentials are being injected. IMPORTANT: <em>DO NOT
 * DIRECTLY INVOKE methods</em> on the client. Use the client to provide method
 * references to be invoked. See example below
 *
 * {@code
 *    CreateStreamResponse call(ProxyClient<KinesisClient> client, CreateStreamRequest request) {
 *        return client.injectCredentialsAndInvokeV2( request,
 * client.client()::createStream); // method reference } }
 *
 * @param <ClientT> the AWS client like KinesisClient that is used to invoke the
 *            web service
 */
public interface ProxyClient<ClientT> {
    /**
     * This is the synchronous version of making API calls.
     *
     * @param request, the AWS service request that we need to make
     * @param requestFunction, this is a Lambda closure that provide the actual API
     *            that needs to be invoked.
     * @param <RequestT> the request type
     * @param <ResponseT> the response from the request
     * @return the response if successful. Else it will propagate all
     *         {@link software.amazon.awssdk.awscore.exception.AwsServiceException}
     *         that is thrown or
     *         {@link software.amazon.awssdk.core.exception.SdkClientException} if
     *         there is client side problem
     *
     */
    <RequestT extends AwsRequest, ResponseT extends AwsResponse>
        ResponseT
        injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction);

    /**
     * This is the asynchronous version of making API calls.
     *
     * @param request, the AWS service request that we need to make
     * @param requestFunction, this is a Lambda closure that provide the actual API
     *            that needs to be invoked.
     * @param <RequestT> the request type
     * @param <ResponseT> the response from the request
     * @return the response if successful. Else it will propagate all
     *         {@link software.amazon.awssdk.awscore.exception.AwsServiceException}
     *         that is thrown or
     *         {@link software.amazon.awssdk.core.exception.SdkClientException} if
     *         there is client side problem
     */
    default <RequestT extends AwsRequest, ResponseT extends AwsResponse>
        CompletableFuture<ResponseT>
        injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a synchronous version of making API calls which implement Iterable in
     * the SDKv2
     *
     * @param request, the AWS service request that we need to make
     * @param requestFunction, this is a Lambda closure that provide the actual API
     *            that needs to be invoked.
     * @param <RequestT> the request type
     * @param <ResponseT> the response from the request
     * @param <IterableT> the iterable collection from the response
     * @return the response if successful. Else it will propagate all
     *         {@link software.amazon.awssdk.awscore.exception.AwsServiceException}
     *         that is thrown or
     *         {@link software.amazon.awssdk.core.exception.SdkClientException} if
     *         there is client side problem
     */
    default <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
        IterableT
        injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a synchronous version of making API calls which implement
     * ResponseInputStream in the SDKv2
     *
     * @param request, the AWS service request that we need to make
     * @param requestFunction, this is a Lambda closure that provide the actual API
     *            that needs to be invoked.
     * @param <RequestT> the request type
     * @param <ResponseT> the response from the request
     * @return the response if successful. Else it will propagate all
     *         {@link software.amazon.awssdk.awscore.exception.AwsServiceException}
     *         that is thrown or
     *         {@link software.amazon.awssdk.core.exception.SdkClientException} if
     *         there is client side problem
     */
    default <RequestT extends AwsRequest, ResponseT extends AwsResponse>
        ResponseInputStream<ResponseT>
        injectCredentialsAndInvokeV2InputStream(RequestT request,
                                                Function<RequestT, ResponseInputStream<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a synchronous version of making API calls which implement
     * ResponseBytes in the SDKv2
     *
     * @param request, the AWS service request that we need to make
     * @param requestFunction, this is a Lambda closure that provide the actual API
     *            that needs to be invoked.
     * @param <RequestT> the request type
     * @param <ResponseT> the response from the request
     * @return the response if successful. Else it will propagate all
     *         {@link software.amazon.awssdk.awscore.exception.AwsServiceException}
     *         that is thrown or
     *         {@link software.amazon.awssdk.core.exception.SdkClientException} if
     *         there is client side problem
     */
    default <RequestT extends AwsRequest, ResponseT extends AwsResponse>
        ResponseBytes<ResponseT>
        injectCredentialsAndInvokeV2Bytes(RequestT request, Function<RequestT, ResponseBytes<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the actual AWS service client that we need to use to provide the
     *         actual method we are going to call.
     */
    ClientT client();
}
