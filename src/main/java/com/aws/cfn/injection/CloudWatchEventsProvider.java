package com.aws.cfn.injection;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsAsyncClient;

public class CloudWatchEventsProvider extends AmazonWebServicesProvider {

    public CloudWatchEventsProvider(final PlatformCredentialsProvider platformCredentialsProvider) {
        super(platformCredentialsProvider);
    }

    public CloudWatchEventsAsyncClient get() {
        return CloudWatchEventsAsyncClient.builder()
            .credentialsProvider(this.getCredentialsProvider())
            .build();
    }
}
