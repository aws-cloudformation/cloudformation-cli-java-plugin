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

import com.amazonaws.cloudformation.injection.CloudWatchEventsLogProvider;
import com.amazonaws.cloudformation.proxy.LoggerProxy;
import com.amazonaws.cloudformation.proxy.MetricsPublisherProxy;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

public class CloudWatchLogPublisherImpl extends LogPublisher {

    private final CloudWatchEventsLogProvider cloudWatchEventsLogProvider;

    private CloudWatchLogsClient cloudWatchLogsClient;
    private String logGroupName;
    private String logStreamName;
    private LoggerProxy loggerProxy;
    private MetricsPublisherProxy metricsPublisherProxy;
    private boolean skipLogging = false;

    public CloudWatchLogPublisherImpl(final CloudWatchEventsLogProvider cloudWatchEventsLogProvider,
                                      final String logGroupName,
                                      final LoggerProxy loggerProxy,
                                      final MetricsPublisherProxy metricsPublisherProxy) {
        this.cloudWatchEventsLogProvider = cloudWatchEventsLogProvider;
        this.logGroupName = logGroupName;
        this.loggerProxy = loggerProxy;
        this.metricsPublisherProxy = metricsPublisherProxy;
    }

    @Override
    public void initialize() {
        try {
            refreshClient();
            createLogGroupIfNotExist();
            this.logStreamName = createLogStream();
        } catch (Exception ex) {
            skipLogging = true;
            loggerProxy.log("Initializing logging group setting failed with error: " + ex.toString());
            emitMetricsForLoggingFailure(ex);
        }
    }

    private void createLogGroupIfNotExist() {
        if (!doesLogGroupExist()) {
            createLogGroup();
        }
    }

    private boolean doesLogGroupExist() {
        DescribeLogGroupsResponse response = cloudWatchLogsClient
            .describeLogGroups(DescribeLogGroupsRequest.builder().logGroupNamePrefix(logGroupName).build());
        Boolean logGroupExists = response.logGroups().stream().filter(logGroup -> logGroup.logGroupName().equals(logGroupName))
            .findAny().isPresent();

        loggerProxy.log(String.format("Log group with name %s does%s exist in resource owner account.", logGroupName,
            logGroupExists ? "" : " not"));
        return logGroupExists;
    }

    private void createLogGroup() {
        loggerProxy.log(String.format("Creating log group with name %s in resource owner account.", logGroupName));
        cloudWatchLogsClient.createLogGroup(CreateLogGroupRequest.builder().logGroupName(logGroupName).build());
    }

    private String createLogStream() {
        String logStreamName = UUID.randomUUID().toString();
        loggerProxy.log(String.format("Creating Log stream with name %s for log group %s.", logStreamName, logGroupName));
        cloudWatchLogsClient
            .createLogStream(CreateLogStreamRequest.builder().logGroupName(logGroupName).logStreamName(logStreamName).build());
        return logStreamName;
    }

    private void refreshClient() {
        if (this.cloudWatchLogsClient == null) {
            this.cloudWatchLogsClient = cloudWatchEventsLogProvider.get();
        }
    }

    @Override
    public boolean publishLogEvent(final String message) {
        try {
            if (skipLogging) {
                return true;
            }
            cloudWatchLogsClient
                .putLogEvents(PutLogEventsRequest.builder().logGroupName(logGroupName).logStreamName(logStreamName)
                    .logEvents(
                        InputLogEvent.builder().message(this.filterMessage(message)).timestamp(new Date().getTime()).build())
                    .build());
            return true;
        } catch (final Exception ex) {
            loggerProxy
                .log(String.format("An error occurred while putting log events [%s] to resource owner account, with error: %s",
                    message, ex.toString()));
            emitMetricsForLoggingFailure(ex);
            return false;
        }
    }

    private void emitMetricsForLoggingFailure(final Exception ex) {
        if (this.metricsPublisherProxy != null) {
            this.metricsPublisherProxy.publishResourceOwnerLogDeliveryExceptionMetric(Instant.now(), ex);
        }
    }
}
