package com.aws.cfn.metrics;

import com.aws.cfn.Action;

import java.time.Instant;

public interface MetricsPublisher {

    String getResourceTypeName();
    void setResourceTypeName(String resourceTypeName);

    void publishExceptionMetric(final Instant timestamp,
                                final Action action,
                                final Exception e);

    void publishInvocationMetric(final Instant timestamp,
                                 final Action action);

    void publishDurationMetric(final Instant timestamp,
                               final Action action,
                               long milliseconds);
}
