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

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MetricsPublisherProxy {
    private final List<MetricsPublisher> metricsPublishers = new ArrayList<>();

    public void addMetricsPublisher(final MetricsPublisher metricsPublisher) {
        metricsPublishers.add(metricsPublisher);
    }

    public void publishExceptionMetric(final Instant timestamp,
                                       final Action action,
                                       final Throwable e,
                                       final HandlerErrorCode handlerErrorCode) {
        metricsPublishers.stream()
            .forEach(metricsPublisher -> metricsPublisher.publishExceptionMetric(timestamp, action, e, handlerErrorCode));
    }

    public void publishInvocationMetric(final Instant timestamp, final Action action) {
        metricsPublishers.stream().forEach(metricsPublisher -> metricsPublisher.publishInvocationMetric(timestamp, action));
    }

    public void publishDurationMetric(final Instant timestamp, final Action action, final long milliseconds) {
        metricsPublishers.stream()
            .forEach(metricsPublisher -> metricsPublisher.publishDurationMetric(timestamp, action, milliseconds));
    }

    public void publishResourceOwnerLogDeliveryExceptionMetric(final Instant timestamp, final Throwable exception) {
        metricsPublishers.stream()
            .forEach(metricsPublisher -> metricsPublisher.publishResourceOwnerLogDeliveryExceptionMetric(timestamp, exception));
    }
}
