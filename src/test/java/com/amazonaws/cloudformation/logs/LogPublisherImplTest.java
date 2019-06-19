package com.amazonaws.cloudformation.logs;

import com.amazonaws.cloudformation.injection.CloudWatchEventsLogProvider;
import com.amazonaws.cloudformation.injection.CloudWatchProvider;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LogPublisherImplTest {

    @Mock
    private CloudWatchEventsLogProvider cloudWatchEventsLogProvider;

    @Mock
    private CloudWatchLogsClient cloudWatchLogsClient;


    @Mock
    private LambdaLogger platformLambdaLogger;

    @Mock
    private CloudWatchProvider platformCloudWatchProvider;

    @Mock
    private CloudWatchProvider resourceOwnerCloudWatchProvider;

    @Mock
    private CloudWatchClient platformCloudWatchClient;

    @Mock
    private CloudWatchClient resourceOwnerCloudWatchClient;

    @Mock
    private LogPublisher resourceOwnerEventsLogger;

    private static final String LOG_GROUP_NAME = "log-group-name";

    @BeforeEach
    public void beforeEach() {
//        when(platformCloudWatchProvider.get()).thenReturn(platformCloudWatchClient);
//        when(platformCloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
//                .thenReturn(mock(PutMetricDataResponse.class));
        when(cloudWatchEventsLogProvider.get()).thenReturn(cloudWatchLogsClient);
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(platformCloudWatchProvider);
        verifyNoMoreInteractions(platformCloudWatchClient);
    }

    @Test
    public void testPublishLogEventsWithExistingLogGroup() {
        final LogPublisherImpl logPublisher = new LogPublisherImpl(cloudWatchEventsLogProvider, LOG_GROUP_NAME, platformLambdaLogger);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor =
                ArgumentCaptor.forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<CreateLogStreamRequest> createLogStreamRequestArgumentCaptor =
                ArgumentCaptor.forClass(CreateLogStreamRequest.class);
        final ArgumentCaptor<PutLogEventsRequest> putLogEventsRequestArgumentCaptor =
                ArgumentCaptor.forClass(PutLogEventsRequest.class);

        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder().logGroups(
                LogGroup.builder()
                        .logGroupName(LOG_GROUP_NAME)
                        .arn("arn:aws:logs:us-east-1:987721315229:log-group:/aws/lambda/testLogGroup-X:*")
                        .creationTime(4567898765l)
                        .storedBytes(456789l).build()
        ).build();

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
                .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture()))
                .thenReturn(null);
        when(cloudWatchLogsClient.putLogEvents(putLogEventsRequestArgumentCaptor.capture()))
                .thenReturn(null);
        final String msgToLog = "How is it going?";
        logPublisher.initializeLoggingConditions();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogStreamRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logStreamName())
                .isEqualTo(createLogStreamRequestArgumentCaptor.getValue().logStreamName());
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logEvents().get(0).message())
                .isEqualTo(msgToLog);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).putLogEvents(putLogEventsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }

    @Test
    public void testPublishLogEventsCreatingNewLogGroup() {
        final LogPublisherImpl logPublisher = new LogPublisherImpl(cloudWatchEventsLogProvider, LOG_GROUP_NAME, platformLambdaLogger);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor =
                ArgumentCaptor.forClass(DescribeLogGroupsRequest.class);
        final ArgumentCaptor<CreateLogStreamRequest> createLogStreamRequestArgumentCaptor =
                ArgumentCaptor.forClass(CreateLogStreamRequest.class);
        final ArgumentCaptor<CreateLogGroupRequest> createLogGroupRequestArgumentCaptor =
                ArgumentCaptor.forClass(CreateLogGroupRequest.class);
        final ArgumentCaptor<PutLogEventsRequest> putLogEventsRequestArgumentCaptor =
                ArgumentCaptor.forClass(PutLogEventsRequest.class);

        final DescribeLogGroupsResponse describeLogGroupsResponse = DescribeLogGroupsResponse.builder().logGroups(ImmutableList.of()).build();

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
                .thenReturn(describeLogGroupsResponse);
        when(cloudWatchLogsClient.createLogGroup(createLogGroupRequestArgumentCaptor.capture()))
                .thenReturn(null);
        when(cloudWatchLogsClient.createLogStream(createLogStreamRequestArgumentCaptor.capture()))
                .thenReturn(null);
        when(cloudWatchLogsClient.putLogEvents(putLogEventsRequestArgumentCaptor.capture()))
                .thenReturn(null);
        final String msgToLog = "How is it going?";
        logPublisher.initializeLoggingConditions();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogGroupRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(createLogStreamRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logStreamName())
                .isEqualTo(createLogStreamRequestArgumentCaptor.getValue().logStreamName());
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logEvents().get(0).message())
                .isEqualTo(msgToLog);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogGroup(createLogGroupRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).createLogStream(createLogStreamRequestArgumentCaptor.getValue());
        verify(cloudWatchLogsClient).putLogEvents(putLogEventsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }

    @Test
    public void testPublishLogEventsSkippedOutOfInitializationFailure() {
        final LogPublisherImpl logPublisher = new LogPublisherImpl(cloudWatchEventsLogProvider, LOG_GROUP_NAME, platformLambdaLogger);
        final ArgumentCaptor<DescribeLogGroupsRequest> describeLogGroupsRequestArgumentCaptor =
                ArgumentCaptor.forClass(DescribeLogGroupsRequest.class);

        when(cloudWatchLogsClient.describeLogGroups(describeLogGroupsRequestArgumentCaptor.capture()))
                .thenThrow(new RuntimeException("Sorry"));

        final String msgToLog = "How is it going?";
        logPublisher.initializeLoggingConditions();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(describeLogGroupsRequestArgumentCaptor.getValue().logGroupNamePrefix()).isEqualTo(LOG_GROUP_NAME);

        verify(cloudWatchLogsClient).describeLogGroups(describeLogGroupsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchEventsLogProvider);
    }
}
