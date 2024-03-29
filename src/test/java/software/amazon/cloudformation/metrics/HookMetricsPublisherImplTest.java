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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.cloudformation.HookInvocationPoint;
import software.amazon.cloudformation.injection.CloudWatchProvider;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

@ExtendWith(MockitoExtension.class)
public class HookMetricsPublisherImplTest {

    @Mock
    private Logger loggerProxy;

    @Mock
    private CloudWatchProvider platformCloudWatchProvider;

    @Mock
    private CloudWatchProvider providerCloudWatchProvider;

    @Mock
    private CloudWatchClient platformCloudWatchClient;

    @Mock
    private CloudWatchClient providerCloudWatchClient;

    private String awsAccountId = "77384178834";
    private final String hookTypeName = "AWS::Test::TestModel";

    @BeforeEach
    public void beforeEach() {
        when(platformCloudWatchProvider.get()).thenReturn(platformCloudWatchClient);
        when(providerCloudWatchProvider.get()).thenReturn(providerCloudWatchClient);
        when(platformCloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
            .thenReturn(mock(PutMetricDataResponse.class));
        when(providerCloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
            .thenReturn(mock(PutMetricDataResponse.class));
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(platformCloudWatchProvider);
        verifyNoMoreInteractions(platformCloudWatchClient);
        verifyNoMoreInteractions(providerCloudWatchProvider);
        verifyNoMoreInteractions(providerCloudWatchClient);
    }

    @Test
    public void testPublishDurationMetric() {
        final HookMetricsPublisherImpl platformMetricsPublisher = new HookMetricsPublisherImpl(platformCloudWatchProvider,
                                                                                               loggerProxy, awsAccountId,
                                                                                               hookTypeName);
        platformMetricsPublisher.refreshClient();

        final HookMetricsPublisherImpl providerMetricsPublisher = new HookMetricsPublisherImpl(providerCloudWatchProvider,
                                                                                               loggerProxy, awsAccountId,
                                                                                               hookTypeName);
        providerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        platformMetricsPublisher.publishDurationMetric(instant, HookInvocationPoint.CREATE_PRE_PROVISION, 123456);
        providerMetricsPublisher.publishDurationMetric(instant, HookInvocationPoint.CREATE_PRE_PROVISION, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        final ArgumentCaptor<PutMetricDataRequest> argument2 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(platformCloudWatchClient).putMetricData(argument1.capture());
        verify(providerCloudWatchClient).putMetricData(argument2.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace())
            .isEqualTo(String.format("%s/%s/%s", "AWS/CloudFormation", awsAccountId, "AWS/Test/TestModel"));

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationDuration");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.MILLISECONDS);
        assertThat(metricDatum.value()).isEqualTo(123456);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(
            Dimension.builder().name("InvocationPoint").value("CREATE_PRE_PROVISION").build(),
            Dimension.builder().name("HookType").value(hookTypeName).build());
    }

    @Test
    public void testPublishExceptionMetric() {
        final HookMetricsPublisherImpl platformMetricsPublisher = new HookMetricsPublisherImpl(platformCloudWatchProvider,
                                                                                               loggerProxy, awsAccountId,
                                                                                               hookTypeName);
        platformMetricsPublisher.refreshClient();

        final HookMetricsPublisherImpl providerMetricsPublisher = new HookMetricsPublisherImpl(providerCloudWatchProvider,
                                                                                               loggerProxy, awsAccountId,
                                                                                               hookTypeName);
        providerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-03T17:50:00Z");
        final RuntimeException e = new RuntimeException("some error");
        platformMetricsPublisher.publishExceptionMetric(instant, HookInvocationPoint.CREATE_PRE_PROVISION, e,
            HandlerErrorCode.InternalFailure);
        providerMetricsPublisher.publishDurationMetric(instant, HookInvocationPoint.DELETE_PRE_PROVISION, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        final ArgumentCaptor<PutMetricDataRequest> argument2 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(platformCloudWatchClient).putMetricData(argument1.capture());
        verify(providerCloudWatchClient).putMetricData(argument2.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace())
            .isEqualTo(String.format("%s/%s/%s", "AWS/CloudFormation", awsAccountId, "AWS/Test/TestModel"));

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerException");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-03T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(
            Dimension.builder().name("InvocationPoint").value("CREATE_PRE_PROVISION").build(),
            Dimension.builder().name("ExceptionType").value("class java.lang.RuntimeException").build(),
            Dimension.builder().name("HookType").value(hookTypeName).build(),
            Dimension.builder().name("HandlerErrorCode").value("InternalFailure").build());
    }

    @Test
    public void testPublishInvocationMetric() {
        final HookMetricsPublisherImpl platformMetricsPublisher = new HookMetricsPublisherImpl(platformCloudWatchProvider,
                                                                                               loggerProxy, awsAccountId,
                                                                                               hookTypeName);
        platformMetricsPublisher.refreshClient();

        final HookMetricsPublisherImpl providerMetricsPublisher = new HookMetricsPublisherImpl(providerCloudWatchProvider,
                                                                                               loggerProxy, awsAccountId,
                                                                                               hookTypeName);
        providerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        platformMetricsPublisher.publishInvocationMetric(instant, HookInvocationPoint.UPDATE_PRE_PROVISION);
        providerMetricsPublisher.publishDurationMetric(instant, HookInvocationPoint.UPDATE_PRE_PROVISION, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        final ArgumentCaptor<PutMetricDataRequest> argument2 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(platformCloudWatchClient).putMetricData(argument1.capture());
        verify(providerCloudWatchClient).putMetricData(argument2.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace())
            .isEqualTo(String.format("%s/%s/%s", "AWS/CloudFormation", awsAccountId, "AWS/Test/TestModel"));

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationCount");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(
            Dimension.builder().name("InvocationPoint").value("UPDATE_PRE_PROVISION").build(),
            Dimension.builder().name("HookType").value(hookTypeName).build());
    }
}
