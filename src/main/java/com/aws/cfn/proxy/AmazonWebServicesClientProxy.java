package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.concurrent.CompletableFuture;

public class AmazonWebServicesClientProxy {

    private final AWSCredentialsProvider v1CredentialsProvider;

    private final AwsCredentialsProvider v2CredentialsProvider;

    private final LambdaLogger logger;

    public AmazonWebServicesClientProxy(
        final LambdaLogger logger,
        final Credentials credentials) {
        this.logger = logger;

        final BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
        this.v1CredentialsProvider = new AWSStaticCredentialsProvider(basicSessionCredentials);

        final AwsSessionCredentials awsSessionCredentials = AwsSessionCredentials.create(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
        this.v2CredentialsProvider = StaticCredentialsProvider.create(awsSessionCredentials);
    }

    public <RequestT extends AmazonWebServiceRequest, ResultT extends AmazonWebServiceResult<ResponseMetadata>> ResultT injectCredentialsAndInvoke(
        final RequestT request,
        final AmazonWebServicesRequestFunction<RequestT, ResultT> requestFunction) {

        request.setRequestCredentialsProvider(v1CredentialsProvider);

        try {
            return requestFunction.apply(request);
        } catch (final Exception e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        } finally {
            request.setRequestCredentialsProvider(null);
        }
    }

    public <RequestT extends AwsRequest, ResultT extends AwsResponse> CompletableFuture<ResultT> injectCredentialsAndInvokeV2(
        final RequestT request,
        final AmazonWebServicesRequestFunctionV2<RequestT, ResultT> requestFunction) {

        final AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
            .credentialsProvider(v2CredentialsProvider)
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
