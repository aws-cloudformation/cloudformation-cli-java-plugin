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

import com.amazonaws.cloudformation.injection.CloudWatchLogsProvider;
import com.amazonaws.cloudformation.proxy.MetricsPublisherProxy;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.time.Instant;
import java.util.Date;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;

public class CloudWatchLogPublisher extends LogPublisher {

    private final CloudWatchLogsProvider cloudWatchLogsProvider;

    private CloudWatchLogsClient cloudWatchLogsClient;
    private String logGroupName;
    private String logStreamName;
    private LambdaLogger platformLambdaLogger;
    private MetricsPublisherProxy metricsPublisherProxy;

    // Note: PutLogEvents returns a result that includes a sequence number.
    // That same sequence number must be used in the subsequent put for the same
    // (log group, log stream) pair.
    // Ref: https://forums.aws.amazon.com/message.jspa?messageID=676799
    private String nextSequenceToken = null;

    public CloudWatchLogPublisher(final CloudWatchLogsProvider cloudWatchLogsProvider,
                                  final String logGroupName,
                                  final String logStreamName,
                                  final LambdaLogger platformLambdaLogger,
                                  final MetricsPublisherProxy metricsPublisherProxy,
                                  final LogFilter... logFilters) {
        super(logFilters);
        this.cloudWatchLogsProvider = cloudWatchLogsProvider;
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        this.platformLambdaLogger = platformLambdaLogger;
        this.metricsPublisherProxy = metricsPublisherProxy;
    }

    public void refreshClient() {
        this.cloudWatchLogsClient = cloudWatchLogsProvider.get();
    }

    @Override
    protected void publishMessage(final String message) {
        try {
            if (skipLogging()) {
                return;
            }
            assert cloudWatchLogsClient != null : "cloudWatchLogsClient was not initialised. "
                + "You must call refreshClient() first.";
            PutLogEventsResponse putLogEventsResponse = cloudWatchLogsClient.putLogEvents(PutLogEventsRequest.builder()
                .sequenceToken(nextSequenceToken).logGroupName(logGroupName).logStreamName(logStreamName)
                .logEvents(InputLogEvent.builder().message(message).timestamp(new Date().getTime()).build()).build());

            nextSequenceToken = putLogEventsResponse.nextSequenceToken();
        } catch (final Exception ex) {
            platformLambdaLogger.log(
                String.format("An error occurred while putting log events [%s] " + "to resource owner account, with error: %s",
                    message, ex.toString()));
            emitMetricsForLoggingFailure(ex);
        }
    }

    private boolean skipLogging() {
        return logStreamName == null;
    }

    private void emitMetricsForLoggingFailure(final Exception ex) {
        if (this.metricsPublisherProxy != null) {
            this.metricsPublisherProxy.publishProviderLogDeliveryExceptionMetric(Instant.now(), ex);
        }
    }
}
