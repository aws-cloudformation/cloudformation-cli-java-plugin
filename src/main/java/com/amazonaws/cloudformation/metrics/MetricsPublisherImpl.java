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
package com.amazonaws.cloudformation.metrics;

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.injection.CloudWatchProvider;
import com.amazonaws.cloudformation.proxy.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

public class MetricsPublisherImpl extends MetricsPublisher {
    private final CloudWatchProvider cloudWatchProvider;

    private Logger loggerProxy;

    private CloudWatchClient cloudWatchClient;

    public MetricsPublisherImpl(final CloudWatchProvider cloudWatchProvider,
                                final Logger loggerProxy) {
        this.cloudWatchProvider = cloudWatchProvider;
        this.loggerProxy = loggerProxy;
    }

    public void refreshClient() {
        this.cloudWatchClient = cloudWatchProvider.get();
    }

    private String getResourceTypeName() {
        return this.resourceTypeName;
    }

    public void publishExceptionMetric(final Instant timestamp, final Action action, final Throwable e) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(Metric.DIMENSION_KEY_ACTION_TYPE, action == null ? "NO_ACTION" : action.name());
        dimensions.put(Metric.DIMENSION_KEY_EXCEPTION_TYPE, e.getClass().toString());
        dimensions.put(Metric.DIMENSION_KEY_RESOURCE_TYPE, this.getResourceTypeName());

        publishMetric(Metric.METRIC_NAME_HANDLER_EXCEPTION, dimensions, StandardUnit.COUNT, 1.0, timestamp);
    }

    public void publishResourceOwnerLogDeliveryExceptionMetric(final Instant timestamp, final Throwable e) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(Metric.DIMENSION_KEY_ACTION_TYPE, "ResourceOwnerLogDelivery");
        dimensions.put(Metric.DIMENSION_KEY_EXCEPTION_TYPE, e.getClass().toString());
        dimensions.put(Metric.DIMENSION_KEY_RESOURCE_TYPE, this.getResourceTypeName());

        publishMetric(Metric.METRIC_NAME_HANDLER_EXCEPTION, dimensions, StandardUnit.COUNT, 1.0, timestamp);
    }

    public void publishInvocationMetric(final Instant timestamp, final Action action) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(Metric.DIMENSION_KEY_ACTION_TYPE, action == null ? "NO_ACTION" : action.name());
        dimensions.put(Metric.DIMENSION_KEY_RESOURCE_TYPE, this.getResourceTypeName());

        publishMetric(Metric.METRIC_NAME_HANDLER_INVOCATION_COUNT, dimensions, StandardUnit.COUNT, 1.0, timestamp);
    }

    public void publishDurationMetric(final Instant timestamp, final Action action, final long milliseconds) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(Metric.DIMENSION_KEY_ACTION_TYPE, action == null ? "NO_ACTION" : action.name());
        dimensions.put(Metric.DIMENSION_KEY_RESOURCE_TYPE, this.getResourceTypeName());

        publishMetric(Metric.METRIC_NAME_HANDLER_DURATION, dimensions, StandardUnit.MILLISECONDS, (double) milliseconds,
            timestamp);
    }

    private void publishMetric(final String metricName,
                               final Map<String, String> dimensionData,
                               final StandardUnit unit,
                               final Double value,
                               final Instant timestamp) {
        assert cloudWatchClient != null : "CloudWatchEventsClient was not initialised. You must call refreshClient() first.";

        List<Dimension> dimensions = new ArrayList<>();
        for (Map.Entry<String, String> kvp : dimensionData.entrySet()) {
            Dimension dimension = Dimension.builder().name(kvp.getKey()).value(kvp.getValue()).build();
            dimensions.add(dimension);
        }

        MetricDatum metricDatum = MetricDatum.builder().metricName(metricName).unit(unit).value(value).dimensions(dimensions)
            .timestamp(timestamp).build();

        PutMetricDataRequest putMetricDataRequest = PutMetricDataRequest.builder()
            .namespace(String.format("%s/%s", Metric.METRIC_NAMESPACE_ROOT, resourceNamespace)).metricData(metricDatum).build();

        try {
            this.cloudWatchClient.putMetricData(putMetricDataRequest);
        } catch (final Exception e) {
            log(String.format("An error occurred while publishing metrics: %s", e.getMessage()));
        }
    }

    private void log(final String message) {
        if (loggerProxy != null) {
            loggerProxy.log(String.format("%s%n", message));
        }
    }
}
