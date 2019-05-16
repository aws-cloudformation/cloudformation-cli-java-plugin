package com.aws.cfn.injection;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

public class CloudWatchProvider extends AmazonWebServicesProvider {

    public CloudWatchProvider(final PlatformCredentialsProvider platformCredentialsProvider) {
        super(platformCredentialsProvider);
    }

    public CloudWatchClient get() {
        return CloudWatchClient.builder()
            .credentialsProvider(this.getCredentialsProvider())
            .build();
    }
}
