package com.aws.cfn.injection;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;

public class CloudWatchEventsProvider extends AmazonWebServicesProvider {

    public CloudWatchEventsProvider(final PlatformCredentialsProvider platformCredentialsProvider) {
        super(platformCredentialsProvider);
    }

    public CloudWatchEventsClient get() {
        return CloudWatchEventsClient.builder()
            .credentialsProvider(this.getCredentialsProvider())
            .build();
    }
}
