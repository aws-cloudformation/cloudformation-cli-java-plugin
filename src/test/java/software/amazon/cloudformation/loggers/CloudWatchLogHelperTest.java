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
package software.amazon.cloudformation.loggers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.cloudformation.injection.CloudWatchLogsProvider;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.MetricsPublisherProxy;

@ExtendWith(MockitoExtension.class)
public class CloudWatchLogHelperTest {

    @Mock
    private CloudWatchLogsProvider cloudWatchLogsProvider;

    @Mock
    private CloudWatchLogsClient cloudWatchLogsClient;

    @Mock
    private LoggerProxy platformLogger;

    @Mock
    private MetricsPublisherProxy metricsPublisherProxy;

    private static final String LOG_GROUP_NAME = "log-group-name";

    @Test
    public void testWithExistingLogGroup() {
        final CloudWatchLogHelper cloudWatchLogHelper = new CloudWatchLogHelper(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                                platformLogger, metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<
            CreateLogStreamRequest> createLogStreamRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);

        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder()
            .logGroups(LogGroup.builder().logGroupName(LOG_GROUP_NAME)
                .arn("arn:aws:loggers:us-east-1:123456789012:log-group:/aws/lambda/testLogGroup-X:*").creationTime(4567898765l)
                .storedBytes(456789l).build())
            .build();

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture())).thenReturn(null);
        cloudWatchLogHelper.refreshClient();
        cloudWatchLogHelper.prepareLogStream();

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogStreamRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchLogsProvider);
    }

    @Test
    public void testWithoutRefreshingClient() {
        final CloudWatchLogHelper cloudWatchLogHelper = new CloudWatchLogHelper(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                                platformLogger, metricsPublisherProxy);
        assertThrows(AssertionError.class, () -> cloudWatchLogHelper.prepareLogStream(), "Expected assertion error");
        verifyNoMoreInteractions(cloudWatchLogsProvider);
    }

    @Test
    public void testWithCreatingNewLogGroup() {
        final CloudWatchLogHelper cloudWatchLogHelper = new CloudWatchLogHelper(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                                platformLogger, metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<
            CreateLogStreamRequest> createLogStreamRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);
        final ArgumentCaptor<
            CreateLogGroupRequest> createLogGroupRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogGroupRequest.class);

        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder()
            .logGroups(ImmutableList.of()).build();

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogGroup(createLogGroupRequestArgumentCaptor.capture())).thenReturn(null);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture())).thenReturn(null);

        cloudWatchLogHelper.refreshClient();
        cloudWatchLogHelper.prepareLogStream();

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogGroupRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogStreamRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogGroup(createLogGroupRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchLogsProvider);
    }

    @Test
    public void testInitialization_DescribeFailure() {
        final CloudWatchLogHelper cloudWatchLogHelper = new CloudWatchLogHelper(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                                platformLogger, metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("Sorry"));

        cloudWatchLogHelper.refreshClient();
        cloudWatchLogHelper.prepareLogStream();

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(metricsPublisherProxy).publishProviderLogDeliveryExceptionMetric(any(), any());
        verify(platformLogger).log(anyString());
        verifyNoMoreInteractions(cloudWatchLogsProvider, platformLogger, metricsPublisherProxy);
    }

    @Test
    public void testInitialization_CreateLogGroupFailure() {
        final CloudWatchLogHelper cloudWatchLogHelper = new CloudWatchLogHelper(cloudWatchLogsProvider, LOG_GROUP_NAME, null,
                                                                                metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<
            CreateLogGroupRequest> createLogGroupRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogGroupRequest.class);
        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder()
            .logGroups(ImmutableList.of()).build();

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogGroup(createLogGroupRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("AccessDenied"));

        cloudWatchLogHelper.refreshClient();
        cloudWatchLogHelper.prepareLogStream();

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(metricsPublisherProxy).publishProviderLogDeliveryExceptionMetric(any(), any());
        verify(platformLogger, times(0)).log(anyString());
        verifyNoMoreInteractions(cloudWatchLogsProvider, platformLogger, metricsPublisherProxy);
    }

    @Test
    public void testInitialization_CreateLogStreamFailure() {
        final CloudWatchLogHelper cloudWatchLogHelper = new CloudWatchLogHelper(cloudWatchLogsProvider, LOG_GROUP_NAME, null,
                                                                                null);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<
            CreateLogGroupRequest> createLogGroupRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogGroupRequest.class);
        final ArgumentCaptor<
            CreateLogStreamRequest> createLogStreamRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);
        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder()
            .logGroups(ImmutableList.of()).build();

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogGroup(createLogGroupRequestArgumentCaptor.capture())).thenReturn(null);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("AccessDenied"));

        cloudWatchLogHelper.refreshClient();
        cloudWatchLogHelper.prepareLogStream();

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).createLogGroup(createLogGroupRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchLogsProvider, platformLogger, metricsPublisherProxy);
    }
}
