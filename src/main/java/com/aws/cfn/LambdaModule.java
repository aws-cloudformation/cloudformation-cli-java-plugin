package com.aws.cfn;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.metrics.MetricsPublisherImpl;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.CloudFormationCallbackAdapter;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Validator;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.google.inject.AbstractModule;

public class LambdaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AmazonCloudFormation.class).to(AmazonCloudFormationClient.class);
        bind(AmazonCloudWatch.class).to(AmazonCloudWatchClient.class);
        bind(MetricsPublisher.class).to(MetricsPublisherImpl.class);
        bind(CallbackAdapter.class).to(CloudFormationCallbackAdapter.class);
        bind(CloudWatchScheduler.class).to(CloudWatchScheduler.class);
        bind(SchemaValidator.class).to(Validator.class);
    }
}