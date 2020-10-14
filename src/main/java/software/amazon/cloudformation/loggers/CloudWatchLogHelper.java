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

import java.time.Instant;
import java.util.UUID;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.cloudformation.injection.CloudWatchLogsProvider;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.MetricsPublisherProxy;

public class CloudWatchLogHelper {

    private final CloudWatchLogsProvider cloudWatchLogsProvider;

    private CloudWatchLogsClient cloudWatchLogsClient;
    private String logGroupName;
    private LoggerProxy platformLogger;
    private MetricsPublisherProxy metricsPublisherProxy;

    public CloudWatchLogHelper(final CloudWatchLogsProvider cloudWatchLogsProvider,
                               final String logGroupName,
                               final LoggerProxy platformLogger,
                               final MetricsPublisherProxy metricsPublisherProxy) {
        this.cloudWatchLogsProvider = cloudWatchLogsProvider;
        this.logGroupName = logGroupName;
        this.platformLogger = platformLogger;
        this.metricsPublisherProxy = metricsPublisherProxy;
    }

    public void refreshClient() {
        this.cloudWatchLogsClient = cloudWatchLogsProvider.get();
    }

    public String prepareLogStream() {
        assert cloudWatchLogsClient != null : "cloudWatchLogsClient was not initialised. You must call refreshClient() first.";
        try {
            if (!doesLogGroupExist()) {
                createLogGroup();
            }
            return createLogStream();
        } catch (Exception ex) {
            log("Initializing logging group setting failed with error: " + ex.toString());
            emitMetricsForLoggingFailure(ex);
        }
        return null;
    }

    private boolean doesLogGroupExist() {
        DescribeLogGroupsResponse response = cloudWatchLogsClient
            .describeLogGroups(DescribeLogGroupsRequest.builder().logGroupNamePrefix(logGroupName).build());
        Boolean logGroupExists = response.logGroups().stream().filter(logGroup -> logGroup.logGroupName().equals(logGroupName))
            .findAny().isPresent();

        log(String.format("Log group with name %s does%s exist in resource owner account.", logGroupName,
            logGroupExists ? "" : " not"));
        return logGroupExists;
    }

    private void createLogGroup() {
        log(String.format("Creating log group with name %s in resource owner account.", logGroupName));
        cloudWatchLogsClient.createLogGroup(CreateLogGroupRequest.builder().logGroupName(logGroupName).build());
    }

    private String createLogStream() {
        String logStreamName = UUID.randomUUID().toString();
        log(String.format("Creating Log stream with name %s for log group %s.", logStreamName, logGroupName));
        cloudWatchLogsClient
            .createLogStream(CreateLogStreamRequest.builder().logGroupName(logGroupName).logStreamName(logStreamName).build());
        return logStreamName;
    }

    private void log(final String message) {
        if (platformLogger != null) {
            platformLogger.log(message);
        }
    }

    private void emitMetricsForLoggingFailure(final Exception ex) {
        if (this.metricsPublisherProxy != null) {
            this.metricsPublisherProxy.publishProviderLogDeliveryExceptionMetric(Instant.now(), ex);
        }
    }
}
