package com.aws.cfn.injection;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

public class CloudWatchProvider extends AmazonWebServicesProvider {

    public CloudWatchProvider(final PlatformCredentialsProvider platformCredentialsProvider) {
        super(platformCredentialsProvider);
    }

    public CloudWatchAsyncClient get() {
        return CloudWatchAsyncClient.builder()
            .credentialsProvider(this.getCredentialsProvider())
            .build();
    }
}
