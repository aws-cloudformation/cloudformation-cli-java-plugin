package com.aws.cfn;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.metrics.MetricsPublisherImpl;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.google.inject.AbstractModule;

public class LambdaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AmazonCloudWatch.class).to(AmazonCloudWatchClient.class);
        bind(MetricsPublisher.class).to(MetricsPublisherImpl.class);
        bind(CloudWatchScheduler.class).to(CloudWatchScheduler.class);
    }
}
