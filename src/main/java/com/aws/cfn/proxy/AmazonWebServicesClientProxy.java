package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;

public class AmazonWebServicesClientProxy {

    private final AWSCredentialsProvider credentialsProvider;

    public AmazonWebServicesClientProxy(final Credentials credentials) {
        final BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
        this.credentialsProvider = new AWSStaticCredentialsProvider(sessionCredentials);
    }

    public AmazonWebServiceResult injectCredentialsAndInvoke(
        final AwsClientBuilder clientBuilder,
        final AmazonWebServiceRequest request,
        final AmazonWebServicesRequestFunction requestFunction) {

        try {
            final AmazonWebServiceClient client = (AmazonWebServiceClient)clientBuilder
                .withCredentials(credentialsProvider)
                .build();
            return requestFunction.apply(client, request);
        } catch (final Exception e) {

        }

        return null;
    }
}
