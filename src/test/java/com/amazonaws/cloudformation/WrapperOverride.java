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
package com.amazonaws.cloudformation;

import com.amazonaws.cloudformation.injection.CredentialsProvider;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.CallbackAdapter;
import com.amazonaws.cloudformation.proxy.HandlerRequest;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.resource.SchemaValidator;
import com.amazonaws.cloudformation.resource.Serializer;
import com.amazonaws.cloudformation.scheduler.CloudWatchScheduler;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Test class used for testing of LambdaWrapper functionality
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WrapperOverride extends LambdaWrapper<TestModel, TestContext> {

    /**
     * Invoked to test normal initialization flows
     */
    public WrapperOverride() {
    }

    /**
     * This .ctor provided for testing
     */
    public WrapperOverride(final CallbackAdapter<TestModel> callbackAdapter,
                           final CredentialsProvider credentialsProvider,
                           final MetricsPublisher metricsPublisher,
                           final CloudWatchScheduler scheduler,
                           final SchemaValidator validator) {
        super(callbackAdapter, credentialsProvider, metricsPublisher, scheduler, validator, new Serializer(),
              new TypeReference<HandlerRequest<TestModel, TestContext>>() {
              });
    }

    @Override
    public InputStream provideResourceSchema() {
        return new ByteArrayInputStream("{ \"properties\": { \"property1\": { \"type\": \"string\" }, \"property2\": { \"type\": \"integer\" } }, \"additionalProperties\": false }"
            .getBytes(Charset.forName("UTF8")));
    }

    @Override
    public ProgressEvent<TestModel, TestContext> invokeHandler(final AmazonWebServicesClientProxy awsClientProxy,
                                                               final ResourceHandlerRequest<TestModel> request,
                                                               final Action action,
                                                               final TestContext callbackContext) throws Exception {
        if (invokeHandlerException != null) {
            throw invokeHandlerException;
        } else if (invokeHandlerResponses == null) {
            return invokeHandlerResponse;
        } else {
            return invokeHandlerResponses.remove();
        }
    }

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

    public ResourceHandlerRequest<TestModel> transformResponse;

    @Override
    protected TypeReference<HandlerRequest<TestModel, TestContext>> getTypeReference() {
        return new TypeReference<HandlerRequest<TestModel, TestContext>>() {
        };
    }
}
