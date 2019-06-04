package com.amazonaws.cloudformation.metrics;

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.injection.CloudWatchProvider;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MetricsPublisherImplTest {

    @Mock
    private LambdaLogger logger;

    @Mock
    private CloudWatchProvider cloudWatchProvider;

    @Mock
    private CloudWatchClient cloudWatchClient;

    @BeforeEach
    public void beforeEach() {
        when(cloudWatchProvider.get()).thenReturn(cloudWatchClient);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
            .thenReturn(mock(PutMetricDataResponse.class));
    }

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(cloudWatchProvider);
        verifyNoMoreInteractions(cloudWatchClient);
    }

    @Test
    public void testPublishDurationMetric() {
        final MetricsPublisherImpl o = new MetricsPublisherImpl(cloudWatchProvider, logger);
        o.setResourceTypeName("AWS::Test::TestModel");
        o.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        o.publishDurationMetric(instant, Action.UPDATE, 123456);

        final ArgumentCaptor<PutMetricDataRequest> argument =
            ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(argument.capture());

        final PutMetricDataRequest request = argument.getValue();
        assertThat(request.namespace()).isEqualTo("AWS_TMP/CloudFormation/AWS/Test/TestModel");

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationDuration");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.MILLISECONDS);
        assertThat(metricDatum.value()).isEqualTo(123456);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(
            Dimension.builder().name("Action").value("UPDATE").build(),
            Dimension.builder().name("ResourceType").value("AWS::Test::TestModel").build()
        );
    }

    @Test
    public void testPublishExceptionMetric() {
        final MetricsPublisherImpl o = new MetricsPublisherImpl(cloudWatchProvider, logger);
        o.setResourceTypeName("AWS::Test::TestModel");
        o.refreshClient();

        final Instant instant = Instant.parse("2019-06-03T17:50:00Z");
        final RuntimeException e = new RuntimeException("some error");
        o.publishExceptionMetric(instant, Action.CREATE, e);

        final ArgumentCaptor<PutMetricDataRequest> argument =
            ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(argument.capture());

        final PutMetricDataRequest request = argument.getValue();
        assertThat(request.namespace()).isEqualTo("AWS_TMP/CloudFormation/AWS/Test/TestModel");

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerException");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-03T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(
            Dimension.builder().name("Action").value("CREATE").build(),
            Dimension.builder().name("ExceptionType").value("class java.lang.RuntimeException").build(),
            Dimension.builder().name("ResourceType").value("AWS::Test::TestModel").build()
        );
    }

    @Test
    public void testPublishInvocationMetric() {
        final MetricsPublisherImpl o = new MetricsPublisherImpl(cloudWatchProvider, logger);
        o.setResourceTypeName("AWS::Test::TestModel");
        o.refreshClient();

        final Instant instant = Instant.parse("2019-06-04T17:50:00Z");
        o.publishInvocationMetric(instant, Action.UPDATE);

        final ArgumentCaptor<PutMetricDataRequest> argument =
            ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(argument.capture());

        final PutMetricDataRequest request = argument.getValue();
        assertThat(request.namespace()).isEqualTo("AWS_TMP/CloudFormation/AWS/Test/TestModel");

        assertThat(request.metricData()).hasSize(1);
        final MetricDatum metricDatum = request.metricData().get(0);
        assertThat(metricDatum.metricName()).isEqualTo("HandlerInvocationCount");
        assertThat(metricDatum.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(metricDatum.value()).isEqualTo(1.0);
        assertThat(metricDatum.timestamp()).isEqualTo(Instant.parse("2019-06-04T17:50:00Z"));
        assertThat(metricDatum.dimensions()).containsExactlyInAnyOrder(
            Dimension.builder().name("Action").value("UPDATE").build(),
            Dimension.builder().name("ResourceType").value("AWS::Test::TestModel").build()
        );
    }
}
