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
package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.injection.CloudFormationProvider;

import java.util.UUID;

import org.json.JSONObject;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressRequest;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressResponse;

public class CloudFormationCallbackAdapter<T> implements CallbackAdapter<T> {

    private final CloudFormationProvider cloudFormationProvider;

    private final LoggerProxy loggerProxy;

    private CloudFormationClient client;

    public CloudFormationCallbackAdapter(final CloudFormationProvider cloudFormationProvider,
                                         final LoggerProxy loggerProxy) {
        this.cloudFormationProvider = cloudFormationProvider;
        this.loggerProxy = loggerProxy;
    }

    public void refreshClient() {
        this.client = cloudFormationProvider.get();
    }

    @Override
    public void reportProgress(final String bearerToken,
                               final HandlerErrorCode errorCode,
                               final OperationStatus operationStatus,
                               final OperationStatus currentOperationStatus,
                               final T resourceModel,
                               final String statusMessage) {
        assert client != null : "CloudWatchEventsClient was not initialised. You must call refreshClient() first.";

        RecordHandlerProgressRequest.Builder requestBuilder = RecordHandlerProgressRequest.builder().bearerToken(bearerToken)
            .operationStatus(translate(operationStatus)).statusMessage(statusMessage)
            .clientRequestToken(UUID.randomUUID().toString());

        if (resourceModel != null) {
            requestBuilder.resourceModel(new JSONObject(resourceModel).toString());
        }

        if (errorCode != null) {
            requestBuilder.errorCode(translate(errorCode));
        }

        if (currentOperationStatus != null) {
            requestBuilder.currentOperationStatus(translate(currentOperationStatus));
        }
        // TODO: be far more fault tolerant, do retries, emit logs and metrics, etc.
        RecordHandlerProgressResponse response = this.client.recordHandlerProgress(requestBuilder.build());
        loggerProxy.log(String.format("Record Handler Progress with Request Id %s and Request: {%s}",
            response.responseMetadata().requestId(), requestBuilder.build().toString()));
    }

    static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode translate(final HandlerErrorCode errorCode) {
        switch (errorCode) {
            case NotUpdatable:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_UPDATABLE;
            case InvalidRequest:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_REQUEST;
            case AccessDenied:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.ACCESS_DENIED;
            case InvalidCredentials:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_CREDENTIALS;
            case AlreadyExists:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.ALREADY_EXISTS;
            case NotFound:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_FOUND;
            case ResourceConflict:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.RESOURCE_CONFLICT;
            case Throttling:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.THROTTLING;
            case ServiceLimitExceeded:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_LIMIT_EXCEEDED;
            case NotStabilized:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_STABILIZED;
            case GeneralServiceException:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.GENERAL_SERVICE_EXCEPTION;
            case ServiceInternalError:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_INTERNAL_ERROR;
            case NetworkFailure:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NETWORK_FAILURE;
            case InternalFailure:
            default:
                // InternalFailure is CloudFormation's fallback error code when no more
                // specificity is there
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INTERNAL_FAILURE;
        }
    }

    private software.amazon.awssdk.services.cloudformation.model.OperationStatus
        translate(final OperationStatus operationStatus) {
        switch (operationStatus) {
            case SUCCESS:
                return software.amazon.awssdk.services.cloudformation.model.OperationStatus.SUCCESS;
            case FAILED:
                return software.amazon.awssdk.services.cloudformation.model.OperationStatus.FAILED;
            case IN_PROGRESS:
                return software.amazon.awssdk.services.cloudformation.model.OperationStatus.IN_PROGRESS;
            case PENDING:
                return software.amazon.awssdk.services.cloudformation.model.OperationStatus.PENDING;
            default:
                // default will be to fail on unknown status
                return software.amazon.awssdk.services.cloudformation.model.OperationStatus.FAILED;
        }
    }
}
