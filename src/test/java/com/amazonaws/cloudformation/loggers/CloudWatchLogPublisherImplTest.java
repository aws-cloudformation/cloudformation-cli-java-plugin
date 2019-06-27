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
package com.amazonaws.cloudformation.loggers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.cloudformation.injection.CloudWatchEventsLogProvider;
import com.amazonaws.cloudformation.injection.CloudWatchProvider;
import com.amazonaws.cloudformation.proxy.LoggerProxy;
import com.amazonaws.cloudformation.proxy.MetricsPublisherProxy;
import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

@ExtendWith(MockitoExtension.class)
public class CloudWatchLogPublisherImplTest {

    @Mock
    private CloudWatchEventsLogProvider cloudWatchEventsLogProvider;

    @Mock
    private CloudWatchLogsClient cloudWatchLogsClient;

    @Mock
    private LoggerProxy loggerProxy;

    @Mock
    private MetricsPublisherProxy metricsPublisherProxy;

    @Mock
    private CloudWatchProvider platformCloudWatchProvider;

    @Mock
    private CloudWatchClient platformCloudWatchClient;

    private static final String LOG_GROUP_NAME = "log-group-name";

    @BeforeEach
    public void beforeEach() {
        when(cloudWatchEventsLogProvider.get()).thenReturn(cloudWatchLogsClient);
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(platformCloudWatchProvider);
        verifyNoMoreInteractions(platformCloudWatchClient);
    }

    @Test
    public void testPublishLogEventsWithExistingLogGroup() {
        final CloudWatchLogPublisherImpl logPublisher = new CloudWatchLogPublisherImpl(cloudWatchEventsLogProvider,
                                                                                       LOG_GROUP_NAME, loggerProxy,
                                                                                       metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<
            CreateLogStreamRequest> createLogStreamRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);
        final ArgumentCaptor<
            PutLogEventsRequest> putLogEventsRequestArgumentCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);

        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder()
            .logGroups(LogGroup.builder().logGroupName(LOG_GROUP_NAME)
                .arn("arn:aws:loggers:us-east-1:987721315229:log-group:/aws/lambda/testLogGroup-X:*").creationTime(4567898765l)
                .storedBytes(456789l).build())
            .build();

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture())).thenReturn(null);
        when(cloudWatchLogsClient.putLogEvents(putLogEventsRequestArgumentCaptor.capture())).thenReturn(null);
        final String msgToLog = "How is it going?";
        logPublisher.initialize();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogStreamRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logStreamName())
            .isEqualTo(createLogStreamRequestArgumentCaptor.getValue().logStreamName());
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logEvents().get(0).message()).isEqualTo(msgToLog);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).putLogEvents(putLogEventsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }

    @Test
    public void testPublishLogEventsCreatingNewLogGroup() {
        final CloudWatchLogPublisherImpl logPublisher = new CloudWatchLogPublisherImpl(cloudWatchEventsLogProvider,
                                                                                       LOG_GROUP_NAME, loggerProxy,
                                                                                       metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<
            CreateLogStreamRequest> createLogStreamRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);
        final ArgumentCaptor<
            CreateLogGroupRequest> createLogGroupRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogGroupRequest.class);
        final ArgumentCaptor<
            PutLogEventsRequest> putLogEventsRequestArgumentCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);

        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder()
            .logGroups(ImmutableList.of()).build();

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogGroup(createLogGroupRequestArgumentCaptor.capture())).thenReturn(null);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture())).thenReturn(null);
        when(cloudWatchLogsClient.putLogEvents(putLogEventsRequestArgumentCaptor.capture())).thenReturn(null);
        final String msgToLog = "How is it going?";
        logPublisher.initialize();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogGroupRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogStreamRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logStreamName())
            .isEqualTo(createLogStreamRequestArgumentCaptor.getValue().logStreamName());
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logEvents().get(0).message()).isEqualTo(msgToLog);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogGroup(createLogGroupRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).putLogEvents(putLogEventsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }

    @Test
    public void testPublishLogEventsSkippedOutOfInitializationFailure() {
        final CloudWatchLogPublisherImpl logPublisher = new CloudWatchLogPublisherImpl(cloudWatchEventsLogProvider,
                                                                                       LOG_GROUP_NAME, loggerProxy,
                                                                                       metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("Sorry"));

        final String msgToLog = "How is it going?";
        logPublisher.initialize();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }

    @Test
    public void testPublishLogEventsSkippedOutOfInitializationFailure_withNullMetricsProxy() {
        final CloudWatchLogPublisherImpl logPublisher = new CloudWatchLogPublisherImpl(cloudWatchEventsLogProvider,
                                                                                       LOG_GROUP_NAME, loggerProxy, null);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("Sorry"));

        final String msgToLog = "How is it going?";
        logPublisher.initialize();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }

    @Test
    public void testPublishLogEventsSkippedOutOfInitializationFailure_errorCreatingLogStream() {
        final CloudWatchLogPublisherImpl logPublisher = new CloudWatchLogPublisherImpl(cloudWatchEventsLogProvider,
                                                                                       LOG_GROUP_NAME, loggerProxy,
                                                                                       metricsPublisherProxy);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor = ArgumentCaptor
            .forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<
            CreateLogStreamRequest> createLogStreamRequestArgumentCaptor = ArgumentCaptor.forClass(CreateLogStreamRequest.class);

        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder()
            .logGroups(LogGroup.builder().logGroupName(LOG_GROUP_NAME)
                .arn("arn:aws:loggers:us-east-1:987721315229:log-group:/aws/lambda/testLogGroup-X:*").creationTime(4567898765l)
                .storedBytes(456789l).build())
            .build();

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
            .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("Error Creating Log Stream"));

        final String msgToLog = "How is it going?";
        logPublisher.initialize();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogStreamRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }
}
