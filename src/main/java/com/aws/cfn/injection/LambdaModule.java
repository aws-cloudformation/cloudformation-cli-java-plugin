package com.aws.cfn.injection;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.metrics.MetricsPublisherImpl;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Validator;
import com.google.inject.AbstractModule;
public class LambdaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AmazonCloudWatch.class).toProvider(AmazonCloudWatchProvider.class);
        bind(AmazonCloudWatchEvents.class).toProvider(AmazonCloudWatchEventsProvider.class);
        bind(MetricsPublisher.class).to(MetricsPublisherImpl.class);
        bind(SchemaValidator.class).to(Validator.class);
    }
}
