package com.aws.cfn.injection;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public abstract class AmazonWebServicesProvider {

    final protected PlatformCredentialsProvider platformCredentialsProvider;

    protected AmazonWebServicesProvider(final PlatformCredentialsProvider platformCredentialsProvider) {
        this.platformCredentialsProvider = platformCredentialsProvider;
    }

    protected AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(this.platformCredentialsProvider.get());
    }
}
