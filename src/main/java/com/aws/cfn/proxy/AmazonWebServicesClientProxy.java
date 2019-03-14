package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class AmazonWebServicesClientProxy {

    private final AWSCredentialsProvider credentialsProvider;
    private final LambdaLogger logger;

    public AmazonWebServicesClientProxy(
        final LambdaLogger logger,
        final Credentials credentials) {
        this.logger = logger;
        final BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
        this.credentialsProvider = new AWSStaticCredentialsProvider(sessionCredentials);
    }

    public <RequestT extends AmazonWebServiceRequest, ResultT extends AmazonWebServiceResult<ResponseMetadata>> ResultT injectCredentialsAndInvoke(
        final RequestT request,
        final AmazonWebServicesRequestFunction<RequestT, ResultT> requestFunction) {

        request.setRequestCredentialsProvider(credentialsProvider);

        try {
            return requestFunction.apply(request);
        } catch (final Exception e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        } finally {
            request.setRequestCredentialsProvider(null);
        }
    }
}
