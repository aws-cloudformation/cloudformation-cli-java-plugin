package com.aws.cfn.metrics;

import com.aws.cfn.Action;

import java.util.Date;

public interface MetricsPublisher {

    String getResourceTypeName();
    void setResourceTypeName(String resourceTypeName);

    void publishInvocationMetric(Date timestamp, Action action);

    void publishDurationMetric(Date timestamp, Action action, long milliseconds);
}
