package com.aws.cfn.injection;

import com.google.inject.Provider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

public class CloudWatchProvider implements Provider<CloudWatchAsyncClient> {

    @Override
    public CloudWatchAsyncClient get() {
        return CloudWatchAsyncClient.create();
    }
}
