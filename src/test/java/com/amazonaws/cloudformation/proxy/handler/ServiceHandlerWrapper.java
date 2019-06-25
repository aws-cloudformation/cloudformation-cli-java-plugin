package com.amazonaws.cloudformation.proxy.handler;

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.LambdaWrapper;
import com.amazonaws.cloudformation.injection.CredentialsProvider;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.CallbackAdapter;
import com.amazonaws.cloudformation.proxy.HandlerErrorCode;
import com.amazonaws.cloudformation.proxy.HandlerRequest;
import com.amazonaws.cloudformation.proxy.LoggerProxy;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.proxy.StdCallbackContext;
import com.amazonaws.cloudformation.proxy.service.ServiceClient;
import com.amazonaws.cloudformation.resource.SchemaValidator;
import com.amazonaws.cloudformation.resource.Serializer;
import com.amazonaws.cloudformation.scheduler.CloudWatchScheduler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

public class ServiceHandlerWrapper extends LambdaWrapper<Model, StdCallbackContext> {

    private final ServiceClient serviceClient;
    public ServiceHandlerWrapper(final CallbackAdapter<Model> callbackAdapter,
                                 final CredentialsProvider credentialsProvider,
                                 final MetricsPublisher metricsPublisher,
                                 final CloudWatchScheduler scheduler,
                                 final SchemaValidator validator,
                                 final Serializer serializer,
                                 final ServiceClient client) {
        super(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator, serializer);
        this.serviceClient = client;
    }

    @Override
    protected ResourceHandlerRequest<Model> transform(final HandlerRequest<Model, StdCallbackContext> request) throws IOException {
        final Model desiredResourceState;
        final Model previousResourceState;

        if (request != null &&
            request.getRequestData() != null &&
            request.getRequestData().getResourceProperties() != null) {
            desiredResourceState = request.getRequestData().getResourceProperties();
        } else {
            desiredResourceState = null;
        }

        if (request != null &&
            request.getRequestData() != null &&
            request.getRequestData().getPreviousResourceProperties() != null) {
            previousResourceState = request.getRequestData().getPreviousResourceProperties();
        } else {
            previousResourceState = null;
        }

        return new ResourceHandlerRequest<>(
            request.getRequestData().getLogicalResourceId(),
            desiredResourceState,
            previousResourceState,
            request.getBearerToken()
        );
    }

    @Override
    protected InputStream provideResourceSchema() {
        return getClass().getResourceAsStream("model.json");
    }

    @Override
    public ProgressEvent<Model, StdCallbackContext> invokeHandler(final AmazonWebServicesClientProxy proxy,
                                                                  final ResourceHandlerRequest<Model> request,
                                                                  final Action action,
                                                                  final StdCallbackContext callbackContext) throws Exception {
        switch (action) {
            case CREATE:
                return new CreateHandler(serviceClient).handleRequest(
                    proxy, request, callbackContext, new LoggerProxy(Mockito.mock(LambdaLogger.class)));

            case READ:
                return new ReadHandler(serviceClient).handleRequest(
                    proxy, request, callbackContext, new LoggerProxy(Mockito.mock(LambdaLogger.class)));

            default:
                return ProgressEvent.failed(
                    request.getDesiredResourceState(),
                    callbackContext,
                    HandlerErrorCode.ServiceException,
                    "Not Implemented");
        }
    }

    @Override
    protected TypeReference<HandlerRequest<Model, StdCallbackContext>> getTypeReference() {
        return new TypeReference<HandlerRequest<Model, StdCallbackContext>>(){};
    }
}
