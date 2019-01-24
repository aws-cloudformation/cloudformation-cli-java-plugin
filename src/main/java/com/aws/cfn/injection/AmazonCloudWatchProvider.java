package com.aws.cfn.injection;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.google.inject.Provider;

public class AmazonCloudWatchProvider implements Provider<AmazonCloudWatch> {

    @Override
    public AmazonCloudWatch get() {
        return AmazonCloudWatchClientBuilder.standard()
            .build();
    }
}
