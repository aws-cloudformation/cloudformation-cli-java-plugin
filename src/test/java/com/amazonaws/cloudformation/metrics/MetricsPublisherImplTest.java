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
package com.amazonaws.cloudformation.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.injection.CloudWatchProvider;
import com.amazonaws.cloudformation.proxy.HandlerErrorCode;
import com.amazonaws.cloudformation.proxy.Logger;

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

@ExtendWith(MockitoExtension.class)
public class MetricsPublisherImplTest {

    @Mock
    private Logger loggerProxy;

    @Mock
    private CloudWatchProvider platformCloudWatchProvider;

    @Mock
    private CloudWatchProvider resourceOwnerCloudWatchProvider;

    @Mock
    private CloudWatchClient platformCloudWatchClient;

    @Mock
    private CloudWatchClient resourceOwnerCloudWatchClient;

    @BeforeEach
    public void beforeEach() {
        when(platformCloudWatchProvider.get()).thenReturn(platformCloudWatchClient);
        when(resourceOwnerCloudWatchProvider.get()).thenReturn(resourceOwnerCloudWatchClient);
        when(platformCloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
            .thenReturn(mock(PutMetricDataResponse.class));
        when(resourceOwnerCloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
            .thenReturn(mock(PutMetricDataResponse.class));
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(platformCloudWatchProvider);
        verifyNoMoreInteractions(platformCloudWatchClient);
        verifyNoMoreInteractions(resourceOwnerCloudWatchProvider);
        verifyNoMoreInteractions(resourceOwnerCloudWatchClient);
    }

    @Test
    public void testPublishDurationMetric() {
        final MetricsPublisherImpl platformMetricsPublisher = new MetricsPublisherImpl(platformCloudWatchProvider, loggerProxy);
        platformMetricsPublisher.setResourceTypeName("AWS::Test::TestModel");
        platformMetricsPublisher.refreshClient();

        final MetricsPublisherImpl resourceOwnerMetricsPublisher = new MetricsPublisherImpl(resourceOwnerCloudWatchProvider,
                                                                                            loggerProxy);
        resourceOwnerMetricsPublisher.setResourceTypeName("AWS::Test::TestModel");
        resourceOwnerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        platformMetricsPublisher.publishDurationMetric(instant, Action.UPDATE, 123456);
        resourceOwnerMetricsPublisher.publishDurationMetric(instant, Action.UPDATE, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        final ArgumentCaptor<PutMetricDataRequest> argument2 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(platformCloudWatchClient).putMetricData(argument1.capture());
        verify(resourceOwnerCloudWatchClient).putMetricData(argument2.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace()).isEqualTo("AWS_TMP/CloudFormation/AWS/Test/TestModel");

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationDuration");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.MILLISECONDS);
        assertThat(metricDatum.value()).isEqualTo(123456);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(Dimension.builder().name("Action").value("UPDATE").build(),
            Dimension.builder().name("ResourceType").value("AWS::Test::TestModel").build());
    }

    @Test
    public void testPublishExceptionMetric() {
        final MetricsPublisherImpl platformMetricsPublisher = new MetricsPublisherImpl(platformCloudWatchProvider, loggerProxy);
        platformMetricsPublisher.setResourceTypeName("AWS::Test::TestModel");
        platformMetricsPublisher.refreshClient();

        final MetricsPublisherImpl resourceOwnerMetricsPublisher = new MetricsPublisherImpl(resourceOwnerCloudWatchProvider,
                                                                                            loggerProxy);
        resourceOwnerMetricsPublisher.setResourceTypeName("AWS::Test::TestModel");
        resourceOwnerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-03T17:50:00Z");
        final RuntimeException e = new RuntimeException("some error");
        platformMetricsPublisher.publishExceptionMetric(instant, Action.CREATE, e, HandlerErrorCode.InternalFailure);
        resourceOwnerMetricsPublisher.publishDurationMetric(instant, Action.UPDATE, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        final ArgumentCaptor<PutMetricDataRequest> argument2 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(platformCloudWatchClient).putMetricData(argument1.capture());
        verify(resourceOwnerCloudWatchClient).putMetricData(argument2.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace()).isEqualTo("AWS_TMP/CloudFormation/AWS/Test/TestModel");

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerException");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-03T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(Dimension.builder().name("Action").value("CREATE").build(),
            Dimension.builder().name("ExceptionType").value("class java.lang.RuntimeException").build(),
            Dimension.builder().name("ResourceType").value("AWS::Test::TestModel").build(),
            Dimension.builder().name("HandlerErrorCode").value("InternalFailure").build());
    }

    @Test
    public void testPublishInvocationMetric() {
        final MetricsPublisherImpl platformMetricsPublisher = new MetricsPublisherImpl(platformCloudWatchProvider, loggerProxy);
        platformMetricsPublisher.setResourceTypeName("AWS::Test::TestModel");
        platformMetricsPublisher.refreshClient();

        final MetricsPublisherImpl resourceOwnerMetricsPublisher = new MetricsPublisherImpl(resourceOwnerCloudWatchProvider,
                                                                                            loggerProxy);
        resourceOwnerMetricsPublisher.setResourceTypeName("AWS::Test::TestModel");
        resourceOwnerMetricsPublisher.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        platformMetricsPublisher.publishInvocationMetric(instant, Action.UPDATE);
        resourceOwnerMetricsPublisher.publishDurationMetric(instant, Action.UPDATE, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument1 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        final ArgumentCaptor<PutMetricDataRequest> argument2 = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(platformCloudWatchClient).putMetricData(argument1.capture());
        verify(resourceOwnerCloudWatchClient).putMetricData(argument2.capture());

        final PutMetricDataRequest request = argument1.getValue();
        assertThat(request.namespace()).isEqualTo("AWS_TMP/CloudFormation/AWS/Test/TestModel");

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationCount");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(Dimension.builder().name("Action").value("UPDATE").build(),
            Dimension.builder().name("ResourceType").value("AWS::Test::TestModel").build());
    }
}
