package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.TestModel;
import com.amazonaws.cloudformation.injection.CloudFormationProvider;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationResponseMetadata;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressRequest;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.ACCESS_DENIED;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INTERNAL_FAILURE;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_CREDENTIALS;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_REQUEST;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NETWORK_FAILURE;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_FOUND;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_READY;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_UPDATABLE;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NO_OPERATION_TO_PERFORM;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_EXCEPTION;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_LIMIT_EXCEEDED;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.SERVICE_TIMEOUT;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.THROTTLING;
import static software.amazon.awssdk.services.cloudformation.model.OperationStatus.FAILED;

@ExtendWith(MockitoExtension.class)
public class CloudFormationCallbackAdapterTest {

    @Mock
    private CloudFormationProvider cloudFormationProvider;

    @Mock
    private LambdaLogger lambdaLogger;

    @Test
    public void testReportProgress() {
        final CloudFormationClient client = mock(CloudFormationClient.class);

        final RecordHandlerProgressResponse response = mock(RecordHandlerProgressResponse.class);
        final CloudFormationResponseMetadata responseMetadata = mock(CloudFormationResponseMetadata.class);
        when(responseMetadata.requestId()).thenReturn(UUID.randomUUID().toString());
        when(response.responseMetadata()).thenReturn(responseMetadata);

        when(cloudFormationProvider.get()).thenReturn(client);

        when(client.recordHandlerProgress(any(RecordHandlerProgressRequest.class)))
            .thenReturn(response);

        final CloudFormationCallbackAdapter<TestModel> adapter =
            new CloudFormationCallbackAdapter<TestModel>(cloudFormationProvider, lambdaLogger);
        adapter.refreshClient();

        adapter.reportProgress(
            "bearer-token",
            HandlerErrorCode.InvalidRequest,
            OperationStatus.FAILED,
            null,
            "some error");

        final ArgumentCaptor<RecordHandlerProgressRequest> argument =
            ArgumentCaptor.forClass(RecordHandlerProgressRequest.class);
        verify(client).recordHandlerProgress(argument.capture());
        assertThat(argument.getValue()).isNotNull();
        assertThat(argument.getValue().bearerToken()).isEqualTo("bearer-token");
        assertThat(argument.getValue().errorCode()).isEqualTo(INVALID_REQUEST);
        assertThat(argument.getValue().operationStatus()).isEqualTo(FAILED);
        assertThat(argument.getValue().resourceModel()).isNull();
        assertThat(argument.getValue().statusMessage()).isEqualTo("some error");
    }

    @Test
    public void testTranslate() {
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.NotUpdatable)).isEqualTo(NOT_UPDATABLE);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.InvalidRequest)).isEqualTo(INVALID_REQUEST);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.AccessDenied)).isEqualTo(ACCESS_DENIED);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.InvalidCredentials)).isEqualTo(INVALID_CREDENTIALS);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.NotFound)).isEqualTo(NOT_FOUND);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.AlreadyExists)).isEqualTo(ALREADY_EXISTS);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.ResourceConflict)).isEqualTo(RESOURCE_CONFLICT);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.Throttling)).isEqualTo(THROTTLING);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.ServiceLimitExceeded)).isEqualTo(SERVICE_LIMIT_EXCEEDED);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.NotStabilized)).isEqualTo(NOT_STABILIZED);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.GeneralServiceException)).isEqualTo(GENERAL_SERVICE_EXCEPTION);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.ServiceInternalError)).isEqualTo(SERVICE_INTERNAL_ERROR);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.NetworkFailure)).isEqualTo(NETWORK_FAILURE);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.InternalFailure)).isEqualTo(INTERNAL_FAILURE);
    }
}
