package com.amazonaws.cloudformation.metrics;

import com.amazonaws.cloudformation.Action;

import java.time.Instant;

public interface MetricsPublisher {

    /**
     * On Lambda re-invoke we need to supply a new set of initiate credentials so this function
     * must be called whenever credentials are refreshed/changed in the owning entity
     */
    void refreshClient();

    String getResourceTypeName();
    void setResourceTypeName(String resourceTypeName);

    void publishExceptionMetric(final Instant timestamp,
                                final Action action,
                                final Throwable e);

    void publishInvocationMetric(final Instant timestamp,
                                 final Action action);

    void publishDurationMetric(final Instant timestamp,
                               final Action action,
                               long milliseconds);
}
