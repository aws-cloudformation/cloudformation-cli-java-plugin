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
package software.amazon.cloudformation.metrics;

import com.google.common.collect.Sets;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.cloudformation.HookInvocationPoint;
import software.amazon.cloudformation.injection.CloudWatchProvider;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

public class HookMetricsPublisherImpl extends MetricsPublisher {
    private final CloudWatchProvider cloudWatchProvider;
    private Logger loggerProxy;
    private String awsAccountId;
    private CloudWatchClient cloudWatchClient;

    public HookMetricsPublisherImpl(final CloudWatchProvider cloudWatchProvider,
                                    final Logger loggerProxy,
                                    final String awsAccountId,
                                    final String hookTypeName) {
        super(hookTypeName);
        this.cloudWatchProvider = cloudWatchProvider;
        this.loggerProxy = loggerProxy;
        this.awsAccountId = awsAccountId;
    }

    @Override
    public void refreshClient() {
        this.cloudWatchClient = cloudWatchProvider.get();
    }

    private String getHookTypeName() {
        return this.typeName;
    }

    @Override
    public void publishExceptionMetric(final Instant timestamp,
                                       final HookInvocationPoint invocationPoint,
                                       final Throwable e,
                                       final HandlerErrorCode handlerErrorCode) {
        publishBulkMetrics(MetricDatum.builder().timestamp(timestamp).metricName(Metric.METRIC_NAME_HANDLER_EXCEPTION)
            .unit(StandardUnit.COUNT).value(1.0)
            .dimensions(Sets.newHashSet(
                Dimension.builder().name(Metric.DIMENSION_KEY_INVOCATION_POINT_TYPE)
                    .value(invocationPoint == null ? "NO_INVOCATION_POINT" : invocationPoint.name()).build(),
                Dimension.builder().name(Metric.DIMENSION_KEY_EXCEPTION_TYPE).value(e.getClass().toString()).build(),
                Dimension.builder().name(Metric.DIMENSION_KEY_HOOK_TYPE).value(this.getHookTypeName()).build(),
                Dimension.builder().name(Metric.DIMENSION_KEY_HANDLER_ERROR_CODE).value(handlerErrorCode.name()).build()))
            .build());
    }

    @Override
    public void publishProviderLogDeliveryExceptionMetric(final Instant timestamp, final Throwable e) {
        publishBulkMetrics(
            MetricDatum.builder().metricName(Metric.METRIC_NAME_HANDLER_EXCEPTION).unit(StandardUnit.COUNT).value(1.0)
                .dimensions(Sets.newHashSet(
                    Dimension.builder().name(Metric.DIMENSION_KEY_INVOCATION_POINT_TYPE).value("HookLogDelivery").build(),
                    Dimension.builder().name(Metric.DIMENSION_KEY_EXCEPTION_TYPE).value(e.getClass().toString()).build(),
                    Dimension.builder().name(Metric.DIMENSION_KEY_HOOK_TYPE).value(this.getHookTypeName()).build()))
                .timestamp(timestamp).build());
    }

    @Override
    public void publishInvocationMetric(final Instant timestamp, final HookInvocationPoint invocationPoint) {
        publishBulkMetrics(
            MetricDatum.builder().metricName(Metric.METRIC_NAME_HANDLER_INVOCATION_COUNT).unit(StandardUnit.COUNT).value(1.0)
                .dimensions(Sets.newHashSet(
                    Dimension.builder().name(Metric.DIMENSION_KEY_INVOCATION_POINT_TYPE)
                        .value(invocationPoint == null ? "NO_INVOCATION_POINT" : invocationPoint.name()).build(),
                    Dimension.builder().name(Metric.DIMENSION_KEY_HOOK_TYPE).value(this.getHookTypeName()).build()))
                .timestamp(timestamp).build());
    }

    @Override
    public void
        publishDurationMetric(final Instant timestamp, final HookInvocationPoint invocationPoint, final long milliseconds) {
        publishBulkMetrics(MetricDatum.builder().metricName(Metric.METRIC_NAME_HANDLER_DURATION).unit(StandardUnit.MILLISECONDS)
            .value((double) milliseconds)
            .dimensions(Sets.newHashSet(
                Dimension.builder().name(Metric.DIMENSION_KEY_INVOCATION_POINT_TYPE)
                    .value(invocationPoint == null ? "NO_INVOCATION_POINT" : invocationPoint.name()).build(),
                Dimension.builder().name(Metric.DIMENSION_KEY_HOOK_TYPE).value(this.getHookTypeName()).build()))
            .timestamp(timestamp).build());
    }

    @Override
    public void publishExceptionByErrorCodeAndCountBulkMetrics(final Instant timestamp,
                                                               final HookInvocationPoint invocationPoint,
                                                               final HandlerErrorCode handlerErrorCode) {
        Collection<MetricDatum> bulkData = new HashSet<>();

        // By Error Code dimensions
        EnumSet.allOf(HandlerErrorCode.class).forEach(errorCode -> {
            bulkData.add(MetricDatum.builder().metricName(Metric.METRIC_NAME_HANDLER_EXCEPTION_BY_ERROR_CODE)
                .unit(StandardUnit.COUNT).value(errorCode == handlerErrorCode ? 1.0 : 0.0)
                .dimensions(Sets.newHashSet(
                    Dimension.builder().name(Metric.DIMENSION_KEY_INVOCATION_POINT_TYPE)
                        .value(invocationPoint == null ? "NO_INVOCATION_POINT" : invocationPoint.name()).build(),
                    Dimension.builder().name(Metric.DIMENSION_KEY_HANDLER_ERROR_CODE).value(errorCode.name()).build()))
                .timestamp(timestamp).build());
        });

        // By Count dimensions
        bulkData.add(MetricDatum.builder().metricName(Metric.METRIC_NAME_HANDLER_EXCEPTION_BY_EXCEPTION_COUNT)
            .unit(StandardUnit.COUNT).value(handlerErrorCode == null ? 0.0 : 1.0)
            .dimensions(Dimension.builder().name(Metric.DIMENSION_KEY_INVOCATION_POINT_TYPE)
                .value(invocationPoint == null ? "NO_INVOCATION_POINT" : invocationPoint.name()).build())
            .timestamp(timestamp).build());

        publishBulkMetrics(bulkData.toArray(new MetricDatum[0]));
    }

    private void publishBulkMetrics(final MetricDatum... metricData) {
        assert cloudWatchClient != null : "CloudWatchEventsClient was not initialised. You must call refreshClient() first.";

        PutMetricDataRequest putMetricDataRequest = PutMetricDataRequest.builder()
            .namespace(String.format("%s/%s/%s", Metric.METRIC_NAMESPACE_ROOT, awsAccountId, namespace)).metricData(metricData)
            .build();

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
