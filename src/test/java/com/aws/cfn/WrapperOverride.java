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
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Test class used for testing of LambdaWrapper functionality
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WrapperOverride extends LambdaWrapper<TestModel, TestContext> {

    /**
     * Invoked to test normal initialization flows
     */
    public WrapperOverride() { }

    /**
     * This .ctor provided for testing
     */
    public WrapperOverride(final CallbackAdapter<TestModel> callbackAdapter,
                           final MetricsPublisher metricsPublisher,
                           final CloudWatchScheduler scheduler,
                           final SchemaValidator validator) {
        super(callbackAdapter, metricsPublisher, scheduler, validator, new Serializer(), new TypeReference<HandlerRequest<TestModel, TestContext>>() {});
    }

    @Override
    public InputStream provideResourceSchema() {
        return new ByteArrayInputStream(
            "{ \"properties\": { \"propertyA\": { \"type\": \"string\" } } }".getBytes(Charset.forName("UTF8")));
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

    public MetricsPublisher metricsPublisher;

    @Override
    protected TypeReference<HandlerRequest<TestModel, TestContext>> getTypeReference() {
        return new TypeReference<HandlerRequest<TestModel, TestContext>>() {};
    }
}
