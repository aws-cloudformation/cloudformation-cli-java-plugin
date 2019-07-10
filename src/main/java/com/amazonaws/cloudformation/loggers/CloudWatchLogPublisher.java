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

public class CloudWatchLogPublisher extends LogPublisher {

    private final CloudWatchLogsProvider cloudWatchLogsProvider;

    private CloudWatchLogsClient cloudWatchLogsClient;
    private String logGroupName;
    private String logStreamName;
    private LambdaLogger platformLambdaLogger;
    private MetricsPublisherProxy metricsPublisherProxy;
    private boolean skipLogging = false;

    public CloudWatchLogPublisher(final CloudWatchLogsProvider cloudWatchLogsProvider,
                                  final String logGroupName,
                                  final String logStreamName,
                                  final LambdaLogger platformLambdaLogger,
                                  final MetricsPublisherProxy metricsPublisherProxy) {
        this.cloudWatchLogsProvider = cloudWatchLogsProvider;
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        this.platformLambdaLogger = platformLambdaLogger;
        this.metricsPublisherProxy = metricsPublisherProxy;
        this.skipLogging = logStreamName == null;
    }

    @Override
    public void refreshClient() {
        this.cloudWatchLogsClient = cloudWatchLogsProvider.get();
    }

    @Override
    protected void publishMessage(final String message) {
        try {
            if (skipLogging) {
                return;
            }
            assert cloudWatchLogsClient != null : "cloudWatchLogsClient was not initialised. "
                + "You must call refreshClient() first.";

            cloudWatchLogsClient
                .putLogEvents(PutLogEventsRequest.builder().logGroupName(logGroupName).logStreamName(logStreamName)
                    .logEvents(InputLogEvent.builder().message(message).timestamp(new Date().getTime()).build()).build());
        } catch (final Exception ex) {
            platformLambdaLogger.log(
                String.format("An error occurred while putting log events [%s] " + "to resource owner account, with error: %s",
                    message, ex.toString()));
            emitMetricsForLoggingFailure(ex);
        }
    }

    private void emitMetricsForLoggingFailure(final Exception ex) {
        if (this.metricsPublisherProxy != null) {
            this.metricsPublisherProxy.publishResourceOwnerLogDeliveryExceptionMetric(Instant.now(), ex);
        }
    }
}
