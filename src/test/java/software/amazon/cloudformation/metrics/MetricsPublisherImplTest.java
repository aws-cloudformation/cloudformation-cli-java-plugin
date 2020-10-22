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
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.injection.CloudWatchProvider;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

@ExtendWith(MockitoExtension.class)
public class MetricsPublisherImplTest {

    @Mock
    private Logger loggerProxy;

    @Mock
    private CloudWatchProvider providerCloudWatchProvider;

    @Mock
    private CloudWatchClient providerCloudWatchClient;

    private final String resourceTypeName = "AWS::Test::TestModel";

    @BeforeEach
    public void beforeEach() {
        when(providerCloudWatchProvider.get()).thenReturn(providerCloudWatchClient);
        when(providerCloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
            .thenReturn(mock(PutMetricDataResponse.class));
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(providerCloudWatchProvider);
        verifyNoMoreInteractions(providerCloudWatchClient);
    }

    @Test
    public void testPublishDurationMetric() {
        final MetricsPublisherImpl providerMetricsPublisher = new MetricsPublisherImpl(providerCloudWatchProvider, loggerProxy,
                                                                                       resourceTypeName);
        providerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        providerMetricsPublisher.publishDurationMetric(instant, Action.UPDATE, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(providerCloudWatchClient).putMetricData(argument1.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace()).isEqualTo(String.format("%s/%s", "AWS/CloudFormation", "AWS/Test/TestModel"));

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationDuration");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.MILLISECONDS);
        assertThat(metricDatum.value()).isEqualTo(123456);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(Dimension.builder().name("Action").value("UPDATE").build(),
            Dimension.builder().name("ResourceType").value(resourceTypeName).build());
    }

    @Test
    public void testPublishExceptionMetric() {
        final MetricsPublisherImpl providerMetricsPublisher = new MetricsPublisherImpl(providerCloudWatchProvider, loggerProxy,
                                                                                       resourceTypeName);
        providerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-03T17:50:00Z");
        final RuntimeException e = new RuntimeException("some error");
        providerMetricsPublisher.publishExceptionMetric(instant, Action.CREATE, e, HandlerErrorCode.InternalFailure);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(providerCloudWatchClient).putMetricData(argument1.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace()).isEqualTo(String.format("%s/%s", "AWS/CloudFormation", "AWS/Test/TestModel"));

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerException");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-03T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(Dimension.builder().name("Action").value("CREATE").build(),
            Dimension.builder().name("ExceptionType").value("class java.lang.RuntimeException").build(),
            Dimension.builder().name("ResourceType").value(resourceTypeName).build(),
            Dimension.builder().name("HandlerErrorCode").value("InternalFailure").build());
    }

    @Test
    public void testPublishInvocationMetric() {
        final MetricsPublisherImpl providerMetricsPublisher = new MetricsPublisherImpl(providerCloudWatchProvider, loggerProxy,
                                                                                       resourceTypeName);
        providerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        providerMetricsPublisher.publishInvocationMetric(instant, Action.UPDATE);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(providerCloudWatchClient).putMetricData(argument1.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace()).isEqualTo(String.format("%s/%s", "AWS/CloudFormation", "AWS/Test/TestModel"));

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationCount");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(Dimension.builder().name("Action").value("UPDATE").build(),
            Dimension.builder().name("ResourceType").value(resourceTypeName).build());
    }
}
