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
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import org.json.JSONObject;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressRequest;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressResponse;

public class CloudFormationCallbackAdapter<T> implements CallbackAdapter<T> {

    private final CloudFormationProvider cloudFormationProvider;

    private final LambdaLogger logger;

    private CloudFormationClient client;

    public CloudFormationCallbackAdapter(final CloudFormationProvider cloudFormationProvider,
                                         final LambdaLogger logger) {
        this.cloudFormationProvider = cloudFormationProvider;
        this.logger = logger;
    }

    public void refreshClient() {
        this.client = cloudFormationProvider.get();
    }

    @Override
    public void reportProgress(final String bearerToken,
                               final HandlerErrorCode errorCode,
                               final OperationStatus operationStatus,
                               final T resourceModel,
                               final String statusMessage) {
        RecordHandlerProgressRequest.Builder requestBuilder = RecordHandlerProgressRequest.builder().bearerToken(bearerToken)
            .operationStatus(translate(operationStatus)).statusMessage(statusMessage);

        if (resourceModel != null) {
            requestBuilder.resourceModel(new JSONObject(resourceModel).toString());
        }

        if (errorCode != null) {
            requestBuilder.errorCode(translate(errorCode));
        }

        // TODO: be far more fault tolerant, do retries, emit logs and metrics, etc.
        RecordHandlerProgressResponse response = this.client.recordHandlerProgress(requestBuilder.build());
        logger.log(String.format("Record Handler Progress with Request Id %s and Request: {%s}",
            response.responseMetadata().requestId(), requestBuilder.build().toString()));
    }

    static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode translate(final HandlerErrorCode errorCode) {
        switch (errorCode) {
            case AccessDenied:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.ACCESS_DENIED;
            case InternalFailure:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INTERNAL_FAILURE;
            case InvalidCredentials:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_CREDENTIALS;
            case InvalidRequest:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_REQUEST;
            case NetworkFailure:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NETWORK_FAILURE;
            case NoOperationToPerform:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NO_OPERATION_TO_PERFORM;
            case NotFound:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_FOUND;
            case NotReady:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_READY;
            case NotUpdatable:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_UPDATABLE;
            case ServiceException:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_EXCEPTION;
            case ServiceLimitExceeded:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_LIMIT_EXCEEDED;
            case ServiceTimeout:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_TIMEOUT;
            case Throttling:
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.THROTTLING;
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
            default:
                // default will be to fail on unknown status
                return software.amazon.awssdk.services.cloudformation.model.OperationStatus.FAILED;
        }
    }
}
