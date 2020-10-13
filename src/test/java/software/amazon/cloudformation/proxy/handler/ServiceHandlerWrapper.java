/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.proxy.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mockito.Mockito;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.cloudformation.AbstractWrapper;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.StdCallbackContext;
import software.amazon.cloudformation.proxy.service.ServiceClient;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;

public class ServiceHandlerWrapper extends AbstractWrapper<Model, StdCallbackContext> {

    private final ServiceClient serviceClient;

    public ServiceHandlerWrapper(final CredentialsProvider providerLoggingCredentialsProvider,
                                 final CloudWatchLogPublisher providerEventsLogger,
                                 final LogPublisher platformEventsLogger,
                                 final MetricsPublisher providerMetricsPublisher,
                                 final SchemaValidator validator,
                                 final Serializer serializer,
                                 final ServiceClient client,
                                 final SdkHttpClient httpClient) {
        super(providerLoggingCredentialsProvider, platformEventsLogger, providerEventsLogger, providerMetricsPublisher, validator,
              serializer, httpClient);
        this.serviceClient = client;
    }

    @Override
    protected ResourceHandlerRequest<Model> transform(final HandlerRequest<Model, StdCallbackContext> request) {
        final Model desiredResourceState;
        final Model previousResourceState;
        final Map<String, String> systemTags;

        if (request != null && request.getRequestData() != null && request.getRequestData().getResourceProperties() != null) {
            desiredResourceState = request.getRequestData().getResourceProperties();
        } else {
            desiredResourceState = null;
        }

        if (request != null && request.getRequestData() != null
            && request.getRequestData().getPreviousResourceProperties() != null) {
            previousResourceState = request.getRequestData().getPreviousResourceProperties();
        } else {
            previousResourceState = null;
        }

        if (request != null && request.getRequestData() != null && request.getRequestData().getSystemTags() != null) {
            systemTags = request.getRequestData().getSystemTags();
        } else {
            systemTags = null;
        }

        return ResourceHandlerRequest.<Model>builder().desiredResourceState(desiredResourceState)
            .previousResourceState(previousResourceState).desiredResourceTags(getDesiredResourceTags(request))
            .systemTags(systemTags).logicalResourceIdentifier(request.getRequestData().getLogicalResourceId())
            .nextToken(request.getNextToken()).snapshotRequested(request.getSnapshotRequested()).build();
    }

    @Override
    protected JSONObject provideResourceSchemaJSONObject() {
        return new JSONObject(new JSONTokener(this.getClass().getResourceAsStream("model.json")));
    }

    @Override
    public Map<String, String> provideResourceDefinedTags(final Model resourceModel) {
        return null;
    }

    @Override
    public ProgressEvent<Model, StdCallbackContext> invokeHandler(final AmazonWebServicesClientProxy proxy,
                                                                  final ResourceHandlerRequest<Model> request,
                                                                  final Action action,
                                                                  final StdCallbackContext callbackContext) {
        switch (action) {
            case CREATE:
                return new CreateHandler(serviceClient).handleRequest(proxy, request, callbackContext,
                    Mockito.mock(LoggerProxy.class));

            case READ:
                return new ReadHandler(serviceClient).handleRequest(proxy, request, callbackContext,
                    Mockito.mock(LoggerProxy.class));

            default:
                return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext,
                    HandlerErrorCode.GeneralServiceException, "Not Implemented");
        }
    }

    @Override
    protected TypeReference<HandlerRequest<Model, StdCallbackContext>> getTypeReference() {
        return new TypeReference<HandlerRequest<Model, StdCallbackContext>>() {
        };
    }

    @Override
    protected TypeReference<Model> getModelTypeReference() {
        return new TypeReference<Model>() {
        };
    }

    @Override
    protected void scrubFiles() {
    }
}
