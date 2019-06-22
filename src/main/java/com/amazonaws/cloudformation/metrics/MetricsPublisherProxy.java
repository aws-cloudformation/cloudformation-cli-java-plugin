package com.amazonaws.cloudformation.metrics;

import com.amazonaws.cloudformation.Action;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MetricsPublisherProxy {
    private final List<MetricsPublisher> metricsPublishers = new ArrayList<>();

    public void addMetricsPublisher(final MetricsPublisher metricsPublisher) {
        metricsPublishers.add(metricsPublisher);
        metricsPublishers.sort(Comparator.comparingInt(MetricsPublisher::getPriority));
    }

    public void setResourceTypeName(final String resourceTypeName) {
        metricsPublishers.stream().forEach(metricsPublisher -> metricsPublisher.setResourceTypeName(
                resourceTypeName
        ));
    }

    public void publishExceptionMetric(final Instant timestamp,
                                       final Action action,
                                       final Throwable e) {
        metricsPublishers.stream().forEach(metricsPublisher -> metricsPublisher.publishExceptionMetric(
                timestamp, action, e
        ));
    }

    public void publishInvocationMetric(final Instant timestamp,
                                        final Action action) {
        metricsPublishers.stream().forEach(metricsPublisher -> metricsPublisher.publishInvocationMetric(
                timestamp, action
        ));
    }

    public void publishDurationMetric(final Instant timestamp,
                                      final Action action,
                                      final long milliseconds) {
        metricsPublishers.stream().forEach(metricsPublisher -> metricsPublisher.publishDurationMetric(
                timestamp, action, milliseconds
        ));
    }

    public void publishResourceOwnerLogDeliveryExceptionMetric(final Instant timestamp, final Throwable exception) {
        metricsPublishers.stream().forEach(metricsPublisher -> metricsPublisher.publishResourceOwnerLogDeliveryExceptionMetric(
                timestamp, exception
        ));
    }
}
