package com.aws.cfn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.aws.cfn.common.FailureMode;
import com.aws.cfn.common.TerminalException;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.aws.rpdk.HandlerRequest;
import com.aws.rpdk.ProgressEvent;
import com.aws.rpdk.RequestContext;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public abstract class LambdaWrapper<T> implements RequestStreamHandler, RequestHandler<Request<T>, Response> {

    private final MetricsPublisher metricsPublisher;
    private final CloudWatchScheduler scheduler;
    private LambdaLogger logger;

    /**
     * This .ctor provided for Lambda runtime which will not automatically invoke Guice injector
     */
    public LambdaWrapper() {
        final Injector injector = Guice.createInjector(new LambdaModule());
        this.metricsPublisher = injector.getInstance(MetricsPublisher.class);
        this.scheduler = injector.getInstance(CloudWatchScheduler.class);
    }

    /**
     * This .ctor provided for testing
     */
    @Inject
    public LambdaWrapper(final MetricsPublisher metricsPublisher,
                         final CloudWatchScheduler scheduler) {
        this.metricsPublisher = metricsPublisher;
        this.scheduler = scheduler;
    }

    public Response handleRequest(final Request request,
                                  final Context context) {
        return null;
    }

    public void handleRequest(final InputStream inputStream,
                              final OutputStream outputStream,
                              final Context context) throws IOException, TerminalException {
        this.logger = context.getLogger();
        this.scheduler.setLogger(context.getLogger());

        if (inputStream == null) {
            writeResponse(
                outputStream,
                createErrorResponse("No request object received.")
            );
            return;
        }

        // decode the input request
        final String input = IOUtils.toString(inputStream, "UTF-8");
        final HandlerRequest<T> request =
            new Gson().fromJson(
                input,
                new TypeToken<HandlerRequest<T>>(){}.getType());

        if (request == null || request.getRequestContext() == null) {
            writeResponse(
                outputStream,
                createErrorResponse(String.format("Invalid request object received (%s)", input))
            );
            return;
        }

        final RequestContext requestContext = request.getRequestContext();

        // If this invocation was triggered by a 're-invoke' CloudWatch Event, clean it up
        this.scheduler.cleanupCloudWatchEvents(
            requestContext.getCloudWatchEventsRuleName(),
            requestContext.getCloudWatchEventsTargetId());

        // MetricsPublisher is initialised with the resource type name for metrics namespace
        this.metricsPublisher.setResourceTypeName(request.getResourceType());

        this.metricsPublisher.publishInvocationMetric(
            Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getResourceRequestType());

        // TODO: implement decryption of request and returned callback context
        // using KMS Key accessible by the Lambda execution Role

        // TODO: Ensure the handler is initialised with;
        // - SDK Client objects injected or via factory
        // - Required caller credentials
        // - Any callback context passed through from prior invocation

        // TODO: Remove this temporary logging
        this.log(String.format("Invocation: %s", requestContext.getInvocation()));
        //this.log(request.getResourceModel().toString());
        this.log(request.getRequestData().getResourceProperties().toString());


        // TODO: implement the handler invocation inside a time check which will abort and automatically
        // reschedule a callback if the handler does not respond within the 15 minute invocation window

        // TODO: ensure that any credential expiry time is also considered in the time check to
        // automatically fail a request if the handler will not be able to complete within that period,
        // such as before a FAS token expires

        final Date startTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        final ProgressEvent handlerResponse = invokeHandler(
            request,
            request.getResourceRequestType(),
            requestContext);

        final Date endTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        metricsPublisher.publishDurationMetric(
            Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getResourceRequestType(),
            (endTime.getTime() - startTime.getTime()));

        // ensure we got a valid response
        if (handlerResponse == null) {
            throw new TerminalException("Handler failed to provide a response.", FailureMode.RESOURCE_UNMODIFIED);
        }

        // When the handler responses InProgress with a callback delay, we trigger a callback to re-invoke
        // the handler for the Resource type to implement stabilization checks and long-poll creation checks
        if (handlerResponse.getStatus() == ProgressStatus.InProgress &&
            handlerResponse.getCallbackDelayMinutes() > 0) {
            final RequestContext callbackContext = new RequestContext();
            callbackContext.setInvocation(requestContext.getInvocation() + 1);
            callbackContext.setCallbackContext(handlerResponse.getCallbackContext());

            this.scheduler.rescheduleAfterMinutes(
                context.getInvokedFunctionArn(),
                handlerResponse.getCallbackDelayMinutes(),
                callbackContext);
        }
        
        // TODO: Implement callback to CloudFormation or specified callback API
        // to report the progress status when in non-terminal state (i.e; InProgress)

        // The wrapper will log any context to the configured CloudWatch log group
        if (handlerResponse.getCallbackContext() != null)
            this.log(handlerResponse.getCallbackContext().toString());

        // A response will be output on all paths, though CloudFormation will
        // not block on invoking the handlers, but rather listen for callbacks
        writeResponse(outputStream, createProgressResponse(handlerResponse));
    }

    private Response createErrorResponse(final String errorMessage) {
        this.log(errorMessage);
        return new Response(errorMessage);
    }

    private Response createProgressResponse(final ProgressEvent progressEvent) {
        this.log(String.format("Got progress %s (%s)\n", progressEvent.getStatus(), progressEvent.getMessage()));
        return new Response(String.format(
            "Got progress %s (%s)",
            progressEvent.getStatus(),
            progressEvent.getMessage()));
    }

    private void writeResponse(final OutputStream outputStream,
                               final Response response) throws IOException {
        outputStream.write(new Gson().toJson(response).getBytes(Charset.forName("UTF-8")));
        outputStream.close();
    }


    public abstract ProgressEvent<T> invokeHandler(final HandlerRequest<T> request,
                                                   final Action action,
                                                   final RequestContext context);

    /**
     * null-safe logger redirect
     * @param message A string containing the event to log.
     */
    private void log(final String message) {
        if (this.logger != null) {
            this.logger.log(message);
        }
    }
}
