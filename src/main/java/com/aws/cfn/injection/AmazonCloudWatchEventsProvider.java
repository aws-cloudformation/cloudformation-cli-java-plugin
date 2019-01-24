package com.aws.cfn.injection;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.google.inject.Provider;

public class AmazonCloudWatchEventsProvider implements Provider<AmazonCloudWatchEvents> {

    @Override
    public AmazonCloudWatchEvents get() {
        return AmazonCloudWatchEventsClientBuilder.standard()
            .build();
    }
}
