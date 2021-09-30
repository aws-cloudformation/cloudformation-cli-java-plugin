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
package software.amazon.cloudformation.proxy.handler.hook;

import com.fasterxml.jackson.core.type.TypeReference;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mockito.Mockito;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.cloudformation.HookInvocationPoint;
import software.amazon.cloudformation.HookLambdaWrapper;
import software.amazon.cloudformation.encryption.KMSCipher;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.StdCallbackContext;
import software.amazon.cloudformation.proxy.handler.Model;
import software.amazon.cloudformation.proxy.handler.TypeConfigurationModel;
import software.amazon.cloudformation.proxy.hook.HookContext;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.HookInvocationRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;
import software.amazon.cloudformation.proxy.service.ServiceClient;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;

public class HookServiceHandlerWrapper extends HookLambdaWrapper<Model, StdCallbackContext, TypeConfigurationModel> {

    private final ServiceClient serviceClient;

    public HookServiceHandlerWrapper(final CredentialsProvider providerLoggingCredentialsProvider,
                                     final CloudWatchLogPublisher providerEventsLogger,
                                     final LogPublisher platformEventsLogger,
                                     final MetricsPublisher platformMetricsPublisher,
                                     final MetricsPublisher providerMetricsPublisher,
                                     final SchemaValidator validator,
                                     final Serializer serializer,
                                     final ServiceClient client,
                                     final SdkHttpClient httpClient,
                                     final KMSCipher cipher) {
        super(providerLoggingCredentialsProvider, providerEventsLogger, platformEventsLogger, platformMetricsPublisher,
              providerMetricsPublisher, validator, serializer, httpClient, cipher);
        this.serviceClient = client;
    }

    @Override
    protected HookHandlerRequest transform(final HookInvocationRequest<TypeConfigurationModel, StdCallbackContext> request) {
        return HookHandlerRequest.builder().clientRequestToken(request.getClientRequestToken())
            .hookContext(HookContext.builder().awsAccountId(request.getAwsAccountId()).stackId(request.getStackId())
                .changeSetId(request.getChangeSetId()).hookTypeName(request.getHookTypeName())
                .hookTypeVersion(request.getHookTypeVersion()).invocationPoint(request.getActionInvocationPoint())
                .targetName(request.getRequestData().getTargetName()).targetType(request.getRequestData().getTargetType())
                .targetLogicalId(request.getRequestData().getTargetLogicalId())
                .targetModel(HookTargetModel.of(request.getRequestData().getTargetModel())).build())
            .build();
    }

    @Override
    protected JSONObject provideHookSchemaJSONObject() {
        return new JSONObject(new JSONTokener(this.getClass().getResourceAsStream("hookModel.json")));
    }

    @Override
    public ProgressEvent<Model, StdCallbackContext> invokeHandler(final AmazonWebServicesClientProxy proxy,
                                                                  final HookHandlerRequest request,
                                                                  final HookInvocationPoint invocationPoint,
                                                                  final StdCallbackContext callbackContext,
                                                                  final TypeConfigurationModel typeConfiguration) {
        if (invocationPoint == HookInvocationPoint.CREATE_PRE_PROVISION) {
            return new PreCreateHandler(serviceClient).handleRequest(proxy, request, callbackContext,
                Mockito.mock(LoggerProxy.class));
        } else {
            return ProgressEvent.failed(null, callbackContext, HandlerErrorCode.GeneralServiceException, "Not Implemented");
        }
    }

    @Override
    protected TypeReference<HookInvocationRequest<TypeConfigurationModel, StdCallbackContext>> getTypeReference() {
        return new TypeReference<HookInvocationRequest<TypeConfigurationModel, StdCallbackContext>>() {
        };
    }

    @Override
    protected TypeReference<TypeConfigurationModel> getModelTypeReference() {
        return new TypeReference<TypeConfigurationModel>() {
        };
    }

    @Override
    protected void scrubFiles() {
    }
}
