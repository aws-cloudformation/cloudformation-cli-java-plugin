package com.aws.cfn.injection;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.metrics.MetricsPublisherImpl;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.CloudFormationCallbackAdapter;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Validator;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class LambdaModule<CallbackT> extends AbstractModule {

    @Override
    protected void configure() {
        bind(AmazonCloudFormation.class).toProvider(AmazonCloudFormationProvider.class);
        bind(AmazonCloudWatch.class).toProvider(AmazonCloudWatchProvider.class);
        bind(AmazonCloudWatchEvents.class).toProvider(AmazonCloudWatchEventsProvider.class);
        bind(MetricsPublisher.class).to(MetricsPublisherImpl.class);
        bind(new TypeLiteral<CallbackAdapter<CallbackT>>() {})
            .to(new TypeLiteral<CloudFormationCallbackAdapter<CallbackT>>() {});
        bind(SchemaValidator.class).to(Validator.class);
    }
}
