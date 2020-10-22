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
package software.amazon.cloudformation.loggers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.cloudformation.injection.CloudWatchLogsProvider;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.MetricsPublisherProxy;

@ExtendWith(MockitoExtension.class)
public class CloudWatchLogPublisherTest {

    @Mock
    private CloudWatchLogsProvider cloudWatchLogsProvider;

    @Mock
    private CloudWatchLogsClient cloudWatchLogsClient;

    @Mock
    private LoggerProxy platformLoggerProxy;

    @Mock
    private MetricsPublisherProxy metricsPublisherProxy;

    private static final String LOG_GROUP_NAME = "log-group-name";
    private static final String LOG_STREAM_NAME = "log-stream-name";

    @Test
    public void testPublishLogEventsHappyCase() {
        final CloudWatchLogPublisher logPublisher = new CloudWatchLogPublisher(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                               LOG_STREAM_NAME, platformLoggerProxy,
                                                                               metricsPublisherProxy);
        final ArgumentCaptor<
            PutLogEventsRequest> putLogEventsRequestArgumentCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.putLogEvents(putLogEventsRequestArgumentCaptor.capture())).thenReturn(null);
        final String msgToLog = "How is it going?";
        logPublisher.refreshClient();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(putLogEventsRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logEvents().get(0).message()).isEqualTo(msgToLog);

        verify(cloudWatchLogsClient).putLogEvents(putLogEventsRequestArgumentCaptor.getValue());
        verifyNoMoreInteractions(cloudWatchLogsProvider);
    }

    @Test
    public void testPublishLogEventsWithError() {
        final CloudWatchLogPublisher logPublisher = new CloudWatchLogPublisher(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                               LOG_STREAM_NAME, platformLoggerProxy,
                                                                               metricsPublisherProxy);
        final ArgumentCaptor<
            PutLogEventsRequest> putLogEventsRequestArgumentCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);
        final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.putLogEvents(putLogEventsRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("AccessDenied"));
        doNothing().when(metricsPublisherProxy).publishProviderLogDeliveryExceptionMetric(any(), any());
        doNothing().when(platformLoggerProxy).log(stringArgumentCaptor.capture());

        final String msgToLog = "How is it going?";
        logPublisher.refreshClient();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(putLogEventsRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logEvents().get(0).message()).isEqualTo(msgToLog);

        verify(cloudWatchLogsClient).putLogEvents(putLogEventsRequestArgumentCaptor.getValue());
        verify(metricsPublisherProxy).publishProviderLogDeliveryExceptionMetric(any(), any());
        assertThat(stringArgumentCaptor.getValue().contains("AccessDenied"));
        verifyNoMoreInteractions(cloudWatchLogsProvider);
    }

    @Test
    public void testPublishLogEventsWithoutRefreshingClient() {
        final CloudWatchLogPublisher logPublisher = new CloudWatchLogPublisher(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                               LOG_STREAM_NAME, platformLoggerProxy,
                                                                               metricsPublisherProxy);
        assertThrows(AssertionError.class, () -> logPublisher.publishLogEvent("How is it going?"), "Expected assertion error");

        verifyNoMoreInteractions(cloudWatchLogsProvider);
    }

    @Test
    public void testLogPublisherWithFilters() {
        final CloudWatchLogPublisher logPublisher = new CloudWatchLogPublisher(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                               LOG_STREAM_NAME, platformLoggerProxy, null);
        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        logPublisher.refreshClient();
        logPublisher.publishLogEvent("This is log message");
    }

    @Test
    public void testPublishLogEventsWithErrorAndNullMetricsPublisher() {
        final CloudWatchLogPublisher logPublisher = new CloudWatchLogPublisher(cloudWatchLogsProvider, LOG_GROUP_NAME,
                                                                               LOG_STREAM_NAME, platformLoggerProxy, null);
        final ArgumentCaptor<
            PutLogEventsRequest> putLogEventsRequestArgumentCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);
        final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        when(cloudWatchLogsClient.putLogEvents(putLogEventsRequestArgumentCaptor.capture()))
            .thenThrow(new RuntimeException("AccessDenied"));
        doNothing().when(platformLoggerProxy).log(stringArgumentCaptor.capture());

        final String msgToLog = "How is it going?";
        logPublisher.refreshClient();
        logPublisher.publishLogEvent(msgToLog);

        assertThat(putLogEventsRequestArgumentCaptor.getValue().logGroupName()).isEqualTo(LOG_GROUP_NAME);
        assertThat(putLogEventsRequestArgumentCaptor.getValue().logEvents().get(0).message()).isEqualTo(msgToLog);

        verify(cloudWatchLogsClient).putLogEvents(putLogEventsRequestArgumentCaptor.getValue());
        verify(metricsPublisherProxy, times(0)).publishProviderLogDeliveryExceptionMetric(any(), any());
        assertThat(stringArgumentCaptor.getValue().contains("AccessDenied"));
        verifyNoMoreInteractions(cloudWatchLogsProvider, metricsPublisherProxy);
    }

    @Test
    public void testPublishLogEventsWithNullLogStream() {
        final CloudWatchLogPublisher logPublisher = new CloudWatchLogPublisher(cloudWatchLogsProvider, LOG_GROUP_NAME, null,
                                                                               platformLoggerProxy, metricsPublisherProxy);
        final String msgToLog = "How is it going?";
        when(cloudWatchLogsProvider.get()).thenReturn(cloudWatchLogsClient);
        logPublisher.refreshClient();
        logPublisher.publishLogEvent(msgToLog);

        verifyNoMoreInteractions(cloudWatchLogsProvider);
    }
}
