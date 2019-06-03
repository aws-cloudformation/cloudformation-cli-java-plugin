package com.amazonaws.cloudformation.metrics;

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.injection.CloudWatchProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsPublisherImpl implements MetricsPublisher {

    private final CloudWatchProvider cloudWatchProvider;

    private CloudWatchClient client;

    private String resourceNamespace;
    private String resourceTypeName;

    public MetricsPublisherImpl(final CloudWatchProvider cloudWatchProvider) {
        this.cloudWatchProvider = cloudWatchProvider;
    }

    public void refreshClient() {
        this.client = cloudWatchProvider.get();
    }

    public String getResourceTypeName() {
        return this.resourceTypeName;
    }

    public void setResourceTypeName(final String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
        this.resourceNamespace = resourceTypeName.replace("::", "/");
    }

    public void publishExceptionMetric(final Instant timestamp,
                                       final Action action,
                                       final Exception e) {
        final Map<String, String> dimensions = new HashMap<>();
        dimensions.put(Metrics.DIMENSION_KEY_ACTION_TYPE, action.name());
        dimensions.put(Metrics.DIMENSION_KEY_EXCEPTION_TYPE, e.getClass().toString());
        dimensions.put(Metrics.DIMENSION_KEY_RESOURCE_TYPE, this.getResourceTypeName());

        publishMetric(Metrics.METRIC_NAME_HANDLER_EXCEPTION,
            dimensions,
            StandardUnit.COUNT,
            1.0,
            timestamp);
    }

    public void publishInvocationMetric(final Instant timestamp,
                                        final Action action) {
        final Map<String, String> dimensions = new HashMap<>();
        dimensions.put(Metrics.DIMENSION_KEY_ACTION_TYPE, action.name());
        dimensions.put(Metrics.DIMENSION_KEY_RESOURCE_TYPE, this.getResourceTypeName());

        publishMetric(
            Metrics.METRIC_NAME_HANDLER_INVOCATION_COUNT,
            dimensions,
            StandardUnit.COUNT,
            1.0,
            timestamp);
    }

    public void publishDurationMetric(final Instant timestamp,
                                      final Action action,
                                      final long milliseconds) {
        final Map<String, String> dimensions = new HashMap<>();
        dimensions.put(Metrics.DIMENSION_KEY_ACTION_TYPE, action.name());
        dimensions.put(Metrics.DIMENSION_KEY_RESOURCE_TYPE, this.getResourceTypeName());

        publishMetric(
            Metrics.METRIC_NAME_HANDLER_DURATION,
            dimensions,
            StandardUnit.MILLISECONDS,
            (double)milliseconds,
            timestamp);
    }

    private void publishMetric(final String metricName,
                               final Map<String, String> dimensionData,
                               final StandardUnit unit,
                               final Double value,
                               final Instant timestamp) {

        final List<Dimension> dimensions = new ArrayList<>();
        for (final Map.Entry<String, String> kvp: dimensionData.entrySet()) {
            final Dimension dimension = Dimension.builder()
                .name(kvp.getKey())
                .value(kvp.getValue())
                .build();
            dimensions.add(dimension);
        }

        final MetricDatum metricDatum = MetricDatum.builder()
            .metricName(metricName)
            .unit(unit)
            .value(value)
            .dimensions(dimensions)
            .timestamp(timestamp)
            .build();

        final PutMetricDataRequest putMetricDataRequest = PutMetricDataRequest.builder()
            .namespace(String.format("%s/%s", Metrics.METRIC_NAMESPACE_ROOT, resourceNamespace))
            .metricData(metricDatum)
            .build();

        client.putMetricData(putMetricDataRequest);
    }
}
