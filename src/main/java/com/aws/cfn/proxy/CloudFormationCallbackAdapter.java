package com.aws.cfn.proxy;

import org.json.JSONObject;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressRequest;

public class CloudFormationCallbackAdapter<T> implements CallbackAdapter<T> {

    private final CloudFormationAsyncClient client;

    public CloudFormationCallbackAdapter(final CloudFormationAsyncClient client) {
        this.client = client;
    }

    @Override
    public void reportProgress(final String bearerToken,
                               final HandlerErrorCode errorCode,
                               final OperationStatus operationStatus,
                               final T resourceModel,
                               final String statusMessage) {
        final RecordHandlerProgressRequest.Builder requestBuilder = RecordHandlerProgressRequest.builder()
            .bearerToken(bearerToken)
            .operationStatus(translate(operationStatus))
            .statusMessage(statusMessage);

        if (resourceModel != null) {
            requestBuilder.resourceModel(new JSONObject(resourceModel).toString());
        }

        if (errorCode != null) {
            requestBuilder.errorCode(translate(errorCode));
        }

        // TODO: be far more fault tolerant, do retries, emit logs and metrics, etc.
        this.client.recordHandlerProgress(requestBuilder.build());
    }

    private software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode translate(
        final HandlerErrorCode errorCode) {
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
                // InternalFailure is CloudFormation's fallback error code when no more specificity is there
                return software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INTERNAL_FAILURE;
        }
    }

    private software.amazon.awssdk.services.cloudformation.model.OperationStatus translate(final OperationStatus operationStatus) {
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
