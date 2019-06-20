package com.amazonaws.cloudformation.logs;

import com.amazonaws.cloudformation.injection.CloudWatchEventsLogProvider;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

import java.util.Date;
import java.util.UUID;

public class LogPublisherImpl implements LogPublisher {

    private final CloudWatchEventsLogProvider cloudWatchEventsLogProvider;

    private CloudWatchLogsClient cloudWatchLogsClient;
    private String logGroupName;
    private String logStreamName;
    private LambdaLogger platformLambdaLogger;
    private boolean skipLogging = false;

    public LogPublisherImpl(final CloudWatchEventsLogProvider cloudWatchEventsLogProvider,
                            final String logGroupName,
                            final LambdaLogger platformLambdaLogger) {
        this.cloudWatchEventsLogProvider = cloudWatchEventsLogProvider;
        this.logGroupName = logGroupName;
        this.platformLambdaLogger = platformLambdaLogger;
    }

    @Override
    public void filterLogMessage() {
        // The way to filter information is to be designed.
    }

    @Override
    public void initialize() {
        try {
            refreshClient();
            createLogGroupIfNotExist();
            this.logStreamName = createLogStream();
        } catch (Throwable ex) {
            skipLogging = true;
            platformLambdaLogger.log("Initializing logging group setting failed with error: " + ex.toString());
        }
    }

    private void createLogGroupIfNotExist() {
        if (!doesLogGroupExist()) {
            createLogGroup();
        }
    }

    private boolean doesLogGroupExist() {
        final DescribeLogGroupsResponse response = cloudWatchLogsClient.describeLogGroups(
                DescribeLogGroupsRequest.builder().logGroupNamePrefix(logGroupName).build());
        final Boolean logGroupExists =
                response.logGroups().stream().filter(
                        logGroup -> logGroup.logGroupName().equals(logGroupName)).findAny().isPresent();

        platformLambdaLogger.log(String.format(
                "Log group with name %s does%s exist in resource owner account.",
                logGroupName,
                logGroupExists ? "": " not"));
        return logGroupExists;
    }

    private void createLogGroup() {
        platformLambdaLogger.log(String.format(
                "Creating Log group with name %s in resource owner account.", logGroupName));
        cloudWatchLogsClient.createLogGroup(CreateLogGroupRequest.builder().logGroupName(logGroupName).build());
    }

    private String createLogStream() {
        final String logStreamName = UUID.randomUUID().toString();
        platformLambdaLogger.log(String.format(
                "Creating Log stream with name %s for log group %s.", logStreamName, logGroupName));
        cloudWatchLogsClient.createLogStream(CreateLogStreamRequest.builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName).build());
        return logStreamName;
    }

    private void refreshClient() {
        if (this.cloudWatchLogsClient == null) {
            this.cloudWatchLogsClient = cloudWatchEventsLogProvider.get();
        }
    }

    @Override
    public void publishLogEvent(final String message) {
        try {
            if (skipLogging) {
                return;
            }
            cloudWatchLogsClient.putLogEvents(PutLogEventsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .logEvents(InputLogEvent.builder().message(message).timestamp(new Date().getTime()).build())
                    .build());
        } catch (final Exception e) {
            platformLambdaLogger.log(String.format(
                    "An error occurred while putting log events [%s] to resource owner account, with error: %s",
                    message, e.toString()));
        }
    }
}
