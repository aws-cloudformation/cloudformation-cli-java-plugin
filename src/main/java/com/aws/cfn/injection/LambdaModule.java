package com.aws.cfn.injection;

import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.metrics.MetricsPublisherImpl;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.CloudFormationCallbackAdapter;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Validator;
import com.google.inject.AbstractModule;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsAsyncClient;

public class LambdaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CloudFormationAsyncClient.class).toProvider(CloudFormationProvider.class);
        bind(CloudWatchAsyncClient.class).toProvider(CloudWatchProvider.class);
        bind(CloudWatchEventsAsyncClient.class).toProvider(CloudWatchEventsProvider.class);
        bind(MetricsPublisher.class).to(MetricsPublisherImpl.class);
        bind(CallbackAdapter.class).to(CloudFormationCallbackAdapter.class);
        bind(SchemaValidator.class).to(Validator.class);
    }
}
