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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_REQUEST;
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
        lenient().when(responseMetadata.requestId()).thenReturn(UUID.randomUUID().toString());
        lenient().when(response.responseMetadata()).thenReturn(responseMetadata);

        lenient().when(cloudFormationProvider.get()).thenReturn(client);

        lenient().when(client.recordHandlerProgress(any(RecordHandlerProgressRequest.class)))
            .thenReturn(response);

        final CloudFormationCallbackAdapter<TestModel> adapter = new CloudFormationCallbackAdapter<>(cloudFormationProvider, lambdaLogger);
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
}
