package com.amazonaws.cloudformation.proxy;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.cloudformation.injection.CloudFormationProvider;
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
        final RecordHandlerProgressResponse response = this.client.recordHandlerProgress(requestBuilder.build());
        logger.log(String.format("Record Handler Progress with Request Id %s and Request: {%s}", response.responseMetadata().requestId(), requestBuilder.build().toString()));
    }

    static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode translate(
        final HandlerErrorCode errorCode) {
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
