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
package software.amazon.cloudformation;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.json.JSONObject;
import org.json.JSONTokener;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;

/**
 * Test class used for testing of LambdaWrapper functionality
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WrapperOverride extends AbstractWrapper<TestModel, TestContext> {

    /**
     * Invoked to test normal initialization flows
     */
    public WrapperOverride(final LogPublisher platformEventsLogger) {
        this.platformLogPublisher = platformEventsLogger;
    }

    /**
     * This .ctor provided for testing
     */
    public WrapperOverride(final CredentialsProvider providerLoggingCredentialsProvider,
                           final LogPublisher platformEventsLogger,
                           final CloudWatchLogPublisher providerEventsLogger,
                           final MetricsPublisher providerMetricsPublisher,
                           final SchemaValidator validator,
                           final SdkHttpClient httpClient) {
        super(providerLoggingCredentialsProvider, platformEventsLogger, providerEventsLogger, providerMetricsPublisher, validator,
              new Serializer(), httpClient);
    }

    @Override
    protected JSONObject provideResourceSchemaJSONObject() {
        return new JSONObject(new JSONTokener(this.getClass().getResourceAsStream("wrapper-override.json")));
    }

    @Override
    public ProgressEvent<TestModel, TestContext> invokeHandler(final AmazonWebServicesClientProxy awsClientProxy,
                                                               final ResourceHandlerRequest<TestModel> request,
                                                               final Action action,
                                                               final TestContext callbackContext)
        throws Exception {
        this.awsClientProxy = awsClientProxy;
        this.request = request;
        this.action = action;
        this.callbackContext = callbackContext;

        if (invokeHandlerException != null) {
            throw invokeHandlerException;
        } else if (invokeHandlerResponses == null) {
            return invokeHandlerResponse;
        } else {
            return invokeHandlerResponses.remove();
        }

    }

    // lets tests assert on the passed in arguments
    public AmazonWebServicesClientProxy awsClientProxy;
    public ResourceHandlerRequest<TestModel> request;
    public Action action;
    public TestContext callbackContext;

    // allows test to have the invoke throw an exception
    public Exception invokeHandlerException;

    // for single mocked response
    public ProgressEvent<TestModel, TestContext> invokeHandlerResponse;

    // for series of mocked responses; call EnqueueResponses to add items
    private Queue<ProgressEvent<TestModel, TestContext>> invokeHandlerResponses;

    public void enqueueResponses(final List<ProgressEvent<TestModel, TestContext>> responses) {
        if (invokeHandlerResponses == null) {
            invokeHandlerResponses = new LinkedList<>();
        }

        invokeHandlerResponses.addAll(responses);
    }

    @Override
    protected ResourceHandlerRequest<TestModel> transform(final HandlerRequest<TestModel, TestContext> request) {
        return transformResponse;
    }

    @Override
    protected Map<String, String> provideResourceDefinedTags(final TestModel resourceModel) {
        return resourceModel.getTags();
    }

    public ResourceHandlerRequest<TestModel> transformResponse;

    @Override
    protected TypeReference<HandlerRequest<TestModel, TestContext>> getTypeReference() {
        return new TypeReference<HandlerRequest<TestModel, TestContext>>() {
        };
    }

    @Override
    protected TypeReference<TestModel> getModelTypeReference() {
        return new TypeReference<TestModel>() {
        };
    }

    @Override
    protected void scrubFiles() {
    }
}
