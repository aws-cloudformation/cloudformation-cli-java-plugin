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

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponse;

public class AmazonWebServicesClientProxy {

    private final AWSCredentialsProvider v1CredentialsProvider;

    private final AwsCredentialsProvider v2CredentialsProvider;

    private final LambdaLogger logger;

    public AmazonWebServicesClientProxy(final LambdaLogger logger,
                                        final Credentials credentials) {
        this.logger = logger;

        BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(credentials.getAccessKeyId(),
                                                                                      credentials.getSecretAccessKey(),
                                                                                      credentials.getSessionToken());
        this.v1CredentialsProvider = new AWSStaticCredentialsProvider(basicSessionCredentials);

        AwsSessionCredentials awsSessionCredentials = AwsSessionCredentials.create(credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(), credentials.getSessionToken());
        this.v2CredentialsProvider = StaticCredentialsProvider.create(awsSessionCredentials);
    }

    public <RequestT extends AmazonWebServiceRequest, ResultT extends AmazonWebServiceResult<ResponseMetadata>>
           ResultT
           injectCredentialsAndInvoke(final RequestT request,
                                      final AmazonWebServicesRequestFunction<RequestT, ResultT> requestFunction) {

        request.setRequestCredentialsProvider(v1CredentialsProvider);

        try {
            return requestFunction.apply(request);
        } catch (final Throwable e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        } finally {
            request.setRequestCredentialsProvider(null);
        }
    }

    public <RequestT extends AwsRequest, ResultT extends AwsResponse>
           ResultT
           injectCredentialsAndInvokeV2(final RequestT request,
                                        final AmazonWebServicesRequestFunctionV2<RequestT, ResultT> requestFunction) {

        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
            .credentialsProvider(v2CredentialsProvider).build();

        @SuppressWarnings("unchecked")
        RequestT wrappedRequest = (RequestT) request.toBuilder().overrideConfiguration(overrideConfiguration).build();

        try {
            return requestFunction.apply(wrappedRequest);
        } catch (final Throwable e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        }
    }

    public <RequestT extends AwsRequest, ResultT extends AwsResponse>
           CompletableFuture<ResultT>
           injectCredentialsAndInvokeV2Async(final RequestT request,
                                             final AmazonWebServicesRequestFunctionV2Async<RequestT, ResultT> requestFunction) {

        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
            .credentialsProvider(v2CredentialsProvider).build();

        @SuppressWarnings("unchecked")
        RequestT wrappedRequest = (RequestT) request.toBuilder().overrideConfiguration(overrideConfiguration).build();

        try {
            return requestFunction.apply(wrappedRequest);
        } catch (final Throwable e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        }
    }
}
