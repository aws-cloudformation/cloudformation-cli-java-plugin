package com.aws.cfn.metrics;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.aws.cfn.Action;
import com.aws.cfn.LambdaModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.util.Date;

public class MetricsPublisherImpl implements MetricsPublisher {

    private final AmazonCloudWatch amazonCloudWatch;
    private String resourceNamespace;
    private String resourceTypeName;

    /**
     * This .ctor provided for Lambda runtime which will not invoke Guice injector
     */
    public MetricsPublisherImpl() {
        final Injector injector = Guice.createInjector(new LambdaModule());
        this.amazonCloudWatch = injector.getInstance(AmazonCloudWatch.class);
    }

    /**
     * This .ctor provided for testing
     * @param amazonCloudWatch
     */
    @Inject
    public MetricsPublisherImpl(final AmazonCloudWatch amazonCloudWatch) {
        this.amazonCloudWatch = amazonCloudWatch;
    }

    public String getResourceTypeName() {
        return this.resourceTypeName;
    }

    public void setResourceTypeName(final String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
        this.resourceNamespace = resourceTypeName.replace("::", "/");
    }

    public void publishInvocationMetric(final Date timestamp, final Action action) {
        publishMetric(
            Metrics.METRIC_NAME_HANDLER_INVOCATION_COUNT,
            "Action",
            action.name(),
            StandardUnit.Count,
            1.0,
            timestamp);
    }

    public void publishDurationMetric(final Date timestamp, final Action action, final long milliseconds) {
        publishMetric(
            Metrics.METRIC_NAME_HANDLER_DURATION,
            "Action",
            action.name(),
            StandardUnit.Milliseconds,
            (double)milliseconds,
            timestamp);
    }

    private void publishMetric(final String metricName,
                               final String dimensionKey,
                               final String dimensionValue,
                               final StandardUnit unit,
                               final Double value,
                               final Date timestamp) {

        final Dimension dimension = new Dimension()
            .withName(dimensionKey)
            .withValue(dimensionValue);

        final MetricDatum metricDatum = new MetricDatum()
            .withMetricName(metricName)
            .withUnit(unit)
            .withValue(value)
            .withDimensions(dimension)
            .withTimestamp(timestamp);

        final PutMetricDataRequest putMetricDataRequest = new PutMetricDataRequest()
            .withNamespace(String.format("%s/%s", Metrics.METRIC_NAMESPACE_ROOT, resourceNamespace))
            .withMetricData(metricDatum);

        amazonCloudWatch.putMetricData(putMetricDataRequest);
    }
}
