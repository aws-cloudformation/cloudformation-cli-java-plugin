package com.aws.cfn.injection;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public abstract class AmazonWebServicesProvider {

    final protected CredentialsProvider credentialsProvider;

    protected AmazonWebServicesProvider(final CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    protected AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(this.credentialsProvider.get());
    }
}
