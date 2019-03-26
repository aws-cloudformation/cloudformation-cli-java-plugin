package com.aws.cfn;

import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.HandlerRequest;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.ResourceHandlerRequest;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Serializer;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Test class used for testing of LambdaWrapper functionality
 * @param <TestModel>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WrapperOverride extends LambdaWrapper<TestModel, TestContext> {


    /**
     * This .ctor provided for testing
     */
    @Inject
    public WrapperOverride(final CallbackAdapter<TestModel> callbackAdapter,
                           final MetricsPublisher metricsPublisher,
                           final CloudWatchScheduler scheduler,
                           final SchemaValidator validator) {
        super(callbackAdapter, metricsPublisher, scheduler, validator, new Serializer());
        typeReference = new TypeReference<HandlerRequest<TestModel, TestContext>>() {};
    }

    @Override
    public InputStream provideResourceSchema() {
        return new ByteArrayInputStream(
            "{ \"properties\": { \"propertyA\": { \"type\": \"string\" } }".getBytes(Charset.forName("UTF8")));
    }

    @Override
    public ProgressEvent<TestModel, TestContext> invokeHandler(final AmazonWebServicesClientProxy awsClientProxy,
                                                  final ResourceHandlerRequest<TestModel> request,
                                                  final Action action,
                                                  final TestContext callbackContext) {
        return invokeHandlerResponse;
    }

    public ProgressEvent<TestModel, TestContext> invokeHandlerResponse;

    @Override
    protected ResourceHandlerRequest<TestModel> transform(final HandlerRequest<TestModel, TestContext> request) {
        return transformResponse;
    }

    public ResourceHandlerRequest<TestModel> transformResponse;
}
