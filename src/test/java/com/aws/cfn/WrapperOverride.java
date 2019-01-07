package com.aws.cfn;

import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.aws.rpdk.HandlerRequest;
import com.aws.rpdk.ProgressEvent;
import com.aws.rpdk.RequestContext;
import com.google.inject.Inject;
import lombok.Data;

/**
 * Test class used for testing of LambdaWrapper functionality
 * @param <TestModel>
 */
@Data
public class WrapperOverride<TestModel> extends LambdaWrapper<TestModel> {

    /**
     * This .ctor provided for testing
     */
    @Inject
    public WrapperOverride(final MetricsPublisher metricsPublisher,
                           final CloudWatchScheduler scheduler) {
        super(metricsPublisher, scheduler);
    }

    @Override
    public ProgressEvent<TestModel> invokeHandler(final HandlerRequest<TestModel> request,
                                                  final Action action,
                                                  final RequestContext context) {
        return invokeHandlerResponse;
    }


    public ProgressEvent<TestModel> invokeHandlerResponse;
}
