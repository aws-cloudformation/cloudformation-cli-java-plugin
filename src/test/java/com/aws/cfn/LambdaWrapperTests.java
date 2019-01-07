package com.aws.cfn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.aws.cfn.common.TerminalException;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.aws.rpdk.ProgressEvent;
import com.aws.rpdk.RequestContext;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LambdaWrapperTests {

    private InputStream loadRequestStream(final String fileName) {
        final File file = new File(fileName);
        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }

        return in;
    }

    private MetricsPublisher getMetricsPublisher() {
        final MetricsPublisher metricsPublisher = mock(MetricsPublisher.class);
        return metricsPublisher;
    }

    private CloudWatchScheduler getScheduler() {
        final CloudWatchScheduler scheduler = mock(CloudWatchScheduler.class);
        return scheduler;
    }

    private Context getLambdaContext() {
        final LambdaLogger lambdaLogger = mock(LambdaLogger.class);

        final Context context = mock(Context.class);
        when(context.getLogger()).thenReturn(lambdaLogger);

        return context;
    }

    @Test(expected= TerminalException.class)
    public void testInvokeHandler_Create_NullResponse() throws IOException {

        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // a null response is a terminal fault
        wrapper.setInvokeHandlerResponse(null);

        final InputStream in = loadRequestStream("src/test/java/com/aws/cfn/request.string");
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        try {
            wrapper.handleRequest(in, out, context);
        }  finally {
            // all metrics should be published even on terminal failure
            verify(metricsPublisher, times(1)).setResourceTypeName(
                "AWS::Test::TestModel");
            verify(metricsPublisher, times(1)).publishInvocationMetric(
                any(Date.class), eq(Action.Create));
            verify(metricsPublisher, times(1)).publishDurationMetric(
                any(Date.class), eq(Action.Create), anyLong());

            // no re-invocation via CloudWatch should occur
            verify(scheduler, times(0)).rescheduleAfterMinutes(
                anyString(), anyInt(), any(RequestContext.class));
            verify(scheduler, times(0)).cleanupCloudWatchEvents(
                anyString(), anyString());

            // verify output response
            assertThat(
                out.toString(),
                is(equalTo("{\\\"message\\\":\\\"Got progress Complete (null)\\\"}"))
            );
        }
    }

    @Test
    public void testInvokeHandler_Create_Complete() throws IOException {

        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // a null response is a terminal fault
        final ProgressEvent<TestModel> pe = new ProgressEvent<>();
        pe.setStatus(ProgressStatus.Complete);
        wrapper.setInvokeHandlerResponse(pe);

        final InputStream in = loadRequestStream("src/test/java/com/aws/cfn/request.string");
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        wrapper.handleRequest(in, out, context);

        // all metrics should be published, once for a single invocation
        verify(metricsPublisher, times(1)).setResourceTypeName(
            "AWS::Test::TestModel");
        verify(metricsPublisher, times(1)).publishInvocationMetric(
            any(Date.class), eq(Action.Create));
        verify(metricsPublisher, times(1)).publishDurationMetric(
            any(Date.class), eq(Action.Create), anyLong());

        // no re-invocation via CloudWatch should occur
        verify(scheduler, times(0)).rescheduleAfterMinutes(
            anyString(), anyInt(), any(RequestContext.class));
        verify(scheduler, times(0)).cleanupCloudWatchEvents(
            anyString(), anyString());

        // verify output response
        assertThat(
            out.toString(),
            is(equalTo("{\"message\":\"Got progress Complete (null)\"}"))
        );
    }

    @Test
    public void testInvokeHandler_Create_Failed() throws IOException {

        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // a null response is a terminal fault
        final ProgressEvent<TestModel> pe = new ProgressEvent<>();
        pe.setMessage("Custom Fault");
        pe.setStatus(ProgressStatus.Failed);
        wrapper.setInvokeHandlerResponse(pe);

        final InputStream in = loadRequestStream("src/test/java/com/aws/cfn/request.string");
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        wrapper.handleRequest(in, out, context);

        // all metrics should be published, once for a single invocation
        verify(metricsPublisher, times(1)).setResourceTypeName(
            "AWS::Test::TestModel");
        verify(metricsPublisher, times(1)).publishInvocationMetric(
            any(Date.class), eq(Action.Create));
        verify(metricsPublisher, times(1)).publishDurationMetric(
            any(Date.class), eq(Action.Create), anyLong());

        // no re-invocation via CloudWatch should occur
        verify(scheduler, times(0)).rescheduleAfterMinutes(
            anyString(), anyInt(), any(RequestContext.class));
        verify(scheduler, times(0)).cleanupCloudWatchEvents(
            anyString(), anyString());

        // verify output response
        assertThat(
            out.toString(),
            is(equalTo("{\"message\":\"Got progress Failed (Custom Fault)\"}"))
        );
    }
}

