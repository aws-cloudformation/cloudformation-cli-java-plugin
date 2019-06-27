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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.*;
import static software.amazon.awssdk.services.cloudformation.model.OperationStatus.FAILED;

import com.amazonaws.cloudformation.TestModel;
import com.amazonaws.cloudformation.injection.CloudFormationProvider;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationResponseMetadata;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressRequest;
import software.amazon.awssdk.services.cloudformation.model.RecordHandlerProgressResponse;

@ExtendWith(MockitoExtension.class)
public class CloudFormationCallbackAdapterTest {

    @Mock
    private CloudFormationProvider cloudFormationProvider;

    @Mock
    private LoggerProxy loggerProxy;

    @Test
    public void testReportProgress() {
        final CloudFormationClient client = mock(CloudFormationClient.class);

        final RecordHandlerProgressResponse response = mock(RecordHandlerProgressResponse.class);
        final CloudFormationResponseMetadata responseMetadata = mock(CloudFormationResponseMetadata.class);
        when(responseMetadata.requestId()).thenReturn(UUID.randomUUID().toString());
        when(response.responseMetadata()).thenReturn(responseMetadata);

        when(cloudFormationProvider.get()).thenReturn(client);

        when(client.recordHandlerProgress(any(RecordHandlerProgressRequest.class))).thenReturn(response);

        final CloudFormationCallbackAdapter<
            TestModel> adapter = new CloudFormationCallbackAdapter<TestModel>(cloudFormationProvider, loggerProxy);
        adapter.refreshClient();

        adapter.reportProgress("bearer-token", HandlerErrorCode.InvalidRequest, OperationStatus.FAILED, null, "some error");

        final ArgumentCaptor<RecordHandlerProgressRequest> argument = ArgumentCaptor.forClass(RecordHandlerProgressRequest.class);
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
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.ServiceLimitExceeded))
            .isEqualTo(SERVICE_LIMIT_EXCEEDED);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.NotStabilized)).isEqualTo(NOT_STABILIZED);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.GeneralServiceException))
            .isEqualTo(GENERAL_SERVICE_EXCEPTION);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.ServiceInternalError))
            .isEqualTo(SERVICE_INTERNAL_ERROR);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.NetworkFailure)).isEqualTo(NETWORK_FAILURE);
        assertThat(CloudFormationCallbackAdapter.translate(HandlerErrorCode.InternalFailure)).isEqualTo(INTERNAL_FAILURE);
    }
}
