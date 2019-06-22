package com.amazonaws.cloudformation.metrics;

import com.amazonaws.cloudformation.Action;

import java.time.Instant;

public abstract class MetricsPublisher {

    protected String resourceTypeName;
    protected String resourceNamespace;

    public void setResourceTypeName(final String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
        this.resourceNamespace = resourceTypeName.replace("::", "/");
    }

    /**
     * On Lambda re-invoke we need to supply a new set of client credentials so this function
     * must be called whenever credentials are refreshed/changed in the owning entity
     */
    public void refreshClient() {}

    /**
     * put metrics priority determines the order when multiple metrics pulisher exist. Default to 100.
     * Smaller number is of higher priority, e.g. priority(0) > prority(10)
     */
    protected int priority = 100;

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public void publishExceptionMetric(final Instant timestamp, final Action action, final Throwable e) {}

    public void publishInvocationMetric(final Instant timestamp, final Action action) {}

    public void publishDurationMetric(final Instant timestamp, final Action action, final long milliseconds) {}

    public void publishResourceOwnerLogDeliveryExceptionMetric(final Instant timestamp, final Throwable exception) {}
}
