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
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.json.JSONObject;
import org.json.JSONTokener;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.cloudformation.encryption.Cipher;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.HookInvocationRequest;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;

/**
 * Test class used for testing of HookLambdaWrapper functionality
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HookWrapperOverride extends HookAbstractWrapper<TestModel, TestContext, TestConfigurationModel> {

    /**
     * Invoked to test normal initialization flows
     */
    public HookWrapperOverride(final LogPublisher platformEventsLogger) {
        this.platformLogPublisher = platformEventsLogger;
    }

    /**
     * Invoked to test normal initialization flows
     */
    public HookWrapperOverride(final LogPublisher platformEventsLogger,
                               final Cipher cipher) {
        this(platformEventsLogger);
        this.cipher = cipher;
    }

    /**
     * This .ctor provided for testing
     */
    public HookWrapperOverride(final CredentialsProvider providerLoggingCredentialsProvider,
                               final LogPublisher platformEventsLogger,
                               final CloudWatchLogPublisher providerEventsLogger,
                               final MetricsPublisher platformMetricsPublisher,
                               final MetricsPublisher providerMetricsPublisher,
                               final SchemaValidator validator,
                               final SdkHttpClient httpClient,
                               final Cipher cipher) {
        super(providerLoggingCredentialsProvider, providerEventsLogger, platformEventsLogger, platformMetricsPublisher,
              providerMetricsPublisher, validator, new Serializer(), httpClient, cipher);
    }

    @Override
    protected JSONObject provideHookSchemaJSONObject() {
        return new JSONObject(new JSONTokener(new ByteArrayInputStream("{ \"properties\": { \"property1\": { \"type\": \"string\" }, \"property2\": { \"type\": \"integer\" } } }"
            .getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public ProgressEvent<TestModel, TestContext> invokeHandler(final AmazonWebServicesClientProxy awsClientProxy,
                                                               final HookHandlerRequest request,
                                                               final HookInvocationPoint invocationPoint,
                                                               final TestContext callbackContext,
                                                               final TestConfigurationModel typeConfiguration)
        throws Exception {
        this.awsClientProxy = awsClientProxy;
        this.request = request;
        this.invocationPoint = invocationPoint;
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
    public HookHandlerRequest request;
    public HookInvocationPoint invocationPoint;
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
    protected HookHandlerRequest transform(final HookInvocationRequest<TestConfigurationModel, TestContext> request) {
        return transformResponse;
    }

    public HookHandlerRequest transformResponse;

    @Override
    protected TypeReference<HookInvocationRequest<TestConfigurationModel, TestContext>> getTypeReference() {
        return new TypeReference<HookInvocationRequest<TestConfigurationModel, TestContext>>() {
        };
    }

    @Override
    protected TypeReference<TestConfigurationModel> getModelTypeReference() {
        return new TypeReference<TestConfigurationModel>() {
        };
    }

    @Override
    protected void scrubFiles() {
    }
}
