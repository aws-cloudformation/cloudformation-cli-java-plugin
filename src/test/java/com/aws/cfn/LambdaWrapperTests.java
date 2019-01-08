package com.aws.cfn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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

    private static final String TEST_DATA_BASE_PATH = "src/test/java/com/aws/cfn/data/%s";

    private InputStream loadRequestStream(final String fileName) {
        final File file = new File(String.format(TEST_DATA_BASE_PATH, fileName));
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
        when(context.getInvokedFunctionArn()).thenReturn("arn:aws:lambda:aws-region:acct-id:function:testHandler:PROD");
        when(context.getLogger()).thenReturn(lambdaLogger);

        return context;
    }

    private void testInvokeHandler_NullResponse(final String requestDataPath,
                                                final Action action) throws IOException {
        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // a null response is a terminal fault
        wrapper.setInvokeHandlerResponse(null);

        final InputStream in = loadRequestStream(requestDataPath);
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        wrapper.handleRequest(in, out, context);

        // all metrics should be published even on terminal failure
        verify(metricsPublisher, times(1)).setResourceTypeName(
            "AWS::Test::TestModel");
        verify(metricsPublisher, times(1)).publishInvocationMetric(
            any(Date.class), eq(action));
        verify(metricsPublisher, times(1)).publishDurationMetric(
            any(Date.class), eq(action), anyLong());

        // no re-invocation via CloudWatch should occur
        verify(scheduler, times(0)).rescheduleAfterMinutes(
            anyString(), anyInt(), any(RequestContext.class));
        verify(scheduler, times(0)).cleanupCloudWatchEvents(
            anyString(), anyString());

        // verify output response
        assertThat(
            out.toString(),
            is(equalTo("{\"status\":\"Failed\",\"message\":\"Handler failed to provide a response.\",\"resourceModel\":{}}"))
        );
    }

    @Test
    public void testInvokeHandler_Create_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("create.request.json", Action.Create);
    }

    @Test
    public void testInvokeHandler_Read_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("read.request.json", Action.Read);
    }

    @Test
    public void testInvokeHandler_Update_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("update.request.json", Action.Update);
    }

    @Test
    public void testInvokeHandler_Delete_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("delete.request.json", Action.Delete);
    }

    @Test
    public void testInvokeHandler_List_NullResponse() throws IOException {
        testInvokeHandler_NullResponse("list.request.json", Action.List);
    }

    private void testInvokeHandler_Failed(final String requestDataPath,
                                          final Action action) throws IOException {

        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // explicit fault response is treated as an unsuccessful synchronous completion
        final ProgressEvent<TestModel> pe = new ProgressEvent<>();
        pe.setMessage("Custom Fault");
        pe.setStatus(ProgressStatus.Failed);
        wrapper.setInvokeHandlerResponse(pe);

        final InputStream in = loadRequestStream(requestDataPath);
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        wrapper.handleRequest(in, out, context);

        // all metrics should be published, once for a single invocation
        verify(metricsPublisher, times(1)).setResourceTypeName(
            "AWS::Test::TestModel");
        verify(metricsPublisher, times(1)).publishInvocationMetric(
            any(Date.class), eq(action));
        verify(metricsPublisher, times(1)).publishDurationMetric(
            any(Date.class), eq(action), anyLong());

        // no re-invocation via CloudWatch should occur
        verify(scheduler, times(0)).rescheduleAfterMinutes(
            anyString(), anyInt(), any(RequestContext.class));
        verify(scheduler, times(0)).cleanupCloudWatchEvents(
            anyString(), anyString());

        // verify output response
        assertThat(
            out.toString(),
            is(equalTo("{\"status\":\"Failed\",\"message\":\"Custom Fault\"}"))
        );
    }

    @Test
    public void testInvokeHandler_Create_Failed() throws IOException {
        testInvokeHandler_Failed("create.request.json", Action.Create);
    }

    @Test
    public void testInvokeHandler_Read_Failed() throws IOException {
        testInvokeHandler_Failed("read.request.json", Action.Read);
    }

    @Test
    public void testInvokeHandler_Update_Failed() throws IOException {
        testInvokeHandler_Failed("update.request.json", Action.Update);
    }

    @Test
    public void testInvokeHandler_Delete_Failed() throws IOException {
        testInvokeHandler_Failed("delete.request.json", Action.Delete);
    }

    @Test
    public void testInvokeHandler_List_Failed() throws IOException {
        testInvokeHandler_Failed("list.request.json", Action.List);
    }

    private void testInvokeHandler_CompleteSynchronously(final String requestDataPath,
                                                        final Action action) throws IOException {

        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // if the handler responds Complete, this is treated as a successful synchronous completion
        final ProgressEvent<TestModel> pe = new ProgressEvent<>();
        pe.setStatus(ProgressStatus.Complete);
        wrapper.setInvokeHandlerResponse(pe);

        final InputStream in = loadRequestStream(requestDataPath);
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        wrapper.handleRequest(in, out, context);

        // all metrics should be published, once for a single invocation
        verify(metricsPublisher, times(1)).setResourceTypeName(
            "AWS::Test::TestModel");
        verify(metricsPublisher, times(1)).publishInvocationMetric(
            any(Date.class), eq(action));
        verify(metricsPublisher, times(1)).publishDurationMetric(
            any(Date.class), eq(action), anyLong());

        // no re-invocation via CloudWatch should occur
        verify(scheduler, times(0)).rescheduleAfterMinutes(
            anyString(), anyInt(), any(RequestContext.class));
        verify(scheduler, times(0)).cleanupCloudWatchEvents(
            anyString(), anyString());

        // verify output response
        assertThat(
            out.toString(),
            is(equalTo("{\"status\":\"Complete\"}"))
        );
    }

    @Test
    public void testInvokeHandler_Create_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("create.request.json", Action.Create);
    }

    @Test
    public void testInvokeHandler_Read_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("read.request.json", Action.Read);
    }

    @Test
    public void testInvokeHandler_Update_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("update.request.json", Action.Update);
    }

    @Test
    public void testInvokeHandler_Delete_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("delete.request.json", Action.Delete);
    }

    @Test
    public void testInvokeHandler_List_CompleteSynchronously() throws IOException {
        testInvokeHandler_CompleteSynchronously("list.request.json", Action.List);
    }

    private void testInvokeHandler_InProgress(final String requestDataPath,
                                              final Action action) throws IOException {

        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel> pe = new ProgressEvent<>();
        pe.setStatus(ProgressStatus.InProgress);
        pe.setResourceModel(new TestModel());
        wrapper.setInvokeHandlerResponse(pe);

        final InputStream in = loadRequestStream(requestDataPath);
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        wrapper.handleRequest(in, out, context);

        // all metrics should be published, once for a single invocation
        verify(metricsPublisher, times(1)).setResourceTypeName(
            "AWS::Test::TestModel");
        verify(metricsPublisher, times(1)).publishInvocationMetric(
            any(Date.class), eq(action));
        verify(metricsPublisher, times(1)).publishDurationMetric(
            any(Date.class), eq(action), anyLong());

        // re-invocation via CloudWatch should occur
        verify(scheduler, times(1)).rescheduleAfterMinutes(
            anyString(), eq(0), any(RequestContext.class));

        // this was a first invocation, so no cleanup is required
        verify(scheduler, times(0)).cleanupCloudWatchEvents(
            anyString(), anyString());

        // CloudFormation should receive a callback invocation
        // TODO

        // verify output response
        assertThat(
            out.toString(),
            is(equalTo("{\"status\":\"InProgress\",\"resourceModel\":{}}"))
        );
    }

    @Test
    public void testInvokeHandler_Create_InProgress() throws IOException {
        testInvokeHandler_InProgress("create.request.json", Action.Create);
    }

    @Test
    public void testInvokeHandler_Read_InProgress() throws IOException {
        testInvokeHandler_InProgress("read.request.json", Action.Read);
    }

    @Test
    public void testInvokeHandler_Update_InProgress() throws IOException {
        testInvokeHandler_InProgress("update.request.json", Action.Update);
    }

    @Test
    public void testInvokeHandler_Delete_InProgress() throws IOException {
        testInvokeHandler_InProgress("delete.request.json", Action.Delete);
    }

    @Test
    public void testInvokeHandler_List_InProgress() throws IOException {
        testInvokeHandler_InProgress("list.request.json", Action.List);
    }

    private void testReInvokeHandler_InProgress(final String requestDataPath,
                                                final Action action) throws IOException {

        final MetricsPublisher metricsPublisher = getMetricsPublisher();
        final CloudWatchScheduler scheduler = getScheduler();
        final WrapperOverride wrapper = new WrapperOverride(metricsPublisher, scheduler);

        // an InProgress response is always re-scheduled.
        // If no explicit time is supplied, a 1-minute interval is used
        final ProgressEvent<TestModel> pe = new ProgressEvent<>();
        pe.setStatus(ProgressStatus.InProgress);
        pe.setResourceModel(new TestModel());
        wrapper.setInvokeHandlerResponse(pe);

        final InputStream in = loadRequestStream(requestDataPath);
        final OutputStream out = new ByteArrayOutputStream();
        final Context context = getLambdaContext();

        wrapper.handleRequest(in, out, context);

        // all metrics should be published, once for a single invocation
        verify(metricsPublisher, times(1)).setResourceTypeName(
            "AWS::Test::TestModel");
        verify(metricsPublisher, times(1)).publishInvocationMetric(
            any(Date.class), eq(action));
        verify(metricsPublisher, times(1)).publishDurationMetric(
            any(Date.class), eq(action), anyLong());

        // re-invocation via CloudWatch should occur
        verify(scheduler, times(1)).rescheduleAfterMinutes(
            anyString(), eq(0), any(RequestContext.class));

        // this was a re-invocation, so a cleanup is required
        verify(scheduler, times(1)).cleanupCloudWatchEvents(
            eq("reinvoke-handler-4754ac8a-623b-45fe-84bc-f5394118a8be"),
            eq("reinvoke-target-4754ac8a-623b-45fe-84bc-f5394118a8be")
        );

        // CloudFormation should receive a callback invocation
        // TODO

        // verify output response
        assertThat(
            out.toString(),
            is(equalTo("{\"status\":\"InProgress\",\"resourceModel\":{}}"))
        );
    }

    @Test
    public void testReInvokeHandler_Create_InProgress() throws IOException {
        testReInvokeHandler_InProgress("create.with-request-context.request.json", Action.Create);
    }

    @Test
    public void testReInvokeHandler_Read_InProgress() throws IOException {
        // TODO: Read handlers must return synchronously so this is probably a fault
        //testReInvokeHandler_InProgress("read.with-request-context.request.json", Action.Read);
    }

    @Test
    public void testReInvokeHandler_Update_InProgress() throws IOException {
        testReInvokeHandler_InProgress("update.with-request-context.request.json", Action.Update);
    }

    @Test
    public void testReInvokeHandler_Delete_InProgress() throws IOException {
        testReInvokeHandler_InProgress("delete.with-request-context.request.json", Action.Delete);
    }

    @Test
    public void testReInvokeHandler_List_InProgress() throws IOException {
        // TODO: List handlers must return synchronously so this is probably a fault
        //testReInvokeHandler_InProgress("list.with-request-context.request.json", Action.List);
    }
}

