package com.aws.cfn.injection;

import com.google.inject.Provider;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsAsyncClient;

public class CloudWatchEventsProvider implements Provider<CloudWatchEventsAsyncClient> {

    @Override
    public CloudWatchEventsAsyncClient get() {
        return CloudWatchEventsAsyncClient.create();
    }
}
