package com.aws.cfn.proxy;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.concurrent.CompletableFuture;

public class AmazonWebServicesClientProxyV2 {

    private final AwsCredentialsProvider credentialsProvider;
    private final LambdaLogger logger;

    public AmazonWebServicesClientProxyV2(
        final LambdaLogger logger,
        final Credentials credentials) {
        this.logger = logger;
        final AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
        this.credentialsProvider = StaticCredentialsProvider.create(sessionCredentials);
    }

    public <RequestT extends AwsRequest, ResultT extends AwsResponse> CompletableFuture<ResultT> injectCredentialsAndInvoke(
        final RequestT request,
        final AmazonWebServicesRequestFunctionV2<RequestT, ResultT> requestFunction) {

        final AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
            .credentialsProvider(credentialsProvider)
            .build();

        final RequestT wrappedRequest = (RequestT) request.toBuilder()
            .overrideConfiguration(overrideConfiguration)
            .build();

        try {
            return requestFunction.apply(wrappedRequest);
        } catch (final Exception e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        }
    }
}
