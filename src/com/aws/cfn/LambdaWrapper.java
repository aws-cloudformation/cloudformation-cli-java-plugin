package com.aws.cfn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.rpdk.HandlerRequest;
import com.aws.rpdk.HandlerRequestImpl;
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
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class LambdaWrapper implements RequestStreamHandler, RequestHandler<Request, Response> {

    private final MetricsPublisher metricsPublisher;
    private LambdaLogger logger;

    /**
     * This .ctor provided for Lambda runtime which will not automatically invoke Guice injector
     */
    public LambdaWrapper() {
        final Injector injector = Guice.createInjector(new LambdaModule());
        this.metricsPublisher = injector.getInstance(MetricsPublisher.class);
    }

    /**
     * This .ctor provided for testing
     */
    @Inject
    public LambdaWrapper(final MetricsPublisher metricsPublisher) {
        this.metricsPublisher = metricsPublisher;
    }

    public Response handleRequest(final Request request,
                                  final Context context) {
       return null;
    }

    public void handleRequest(final InputStream inputStream,
                              final OutputStream outputStream,
                              final Context context) throws IOException {
        this.logger = context.getLogger();

        if (inputStream == null ) {
            writeResponse(
                outputStream,
                createErrorResponse("No request object received.")
            );
            return;
        }

        // decode the input request
        final String input = IOUtils.toString(inputStream, "UTF-8");
        final HandlerRequest<?> request =
            new Gson().fromJson(
                input,
                new TypeToken<HandlerRequestImpl<?>>(){}.getType());

        if (request == null || request.getRequestContext() == null) {
            writeResponse(
                outputStream,
                createErrorResponse(String.format("Invalid request object received (%s)", input))
            );
            return;
        }

        final RequestContext requestContext = request.getRequestContext();

        // MetricsPublisher is initialised with the resource type name for metrics namespace
        this.metricsPublisher.setResourceTypeName(requestContext.getResourceType());

        this.metricsPublisher.publishInvocationMetric(
            Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getAction());

        // TODO: implement decryption of request and returned callback context
        // using KMS Key accessible by the Lambda execution Role

        // TODO: Ensure the handler is initialised with;
        // - SDK Client objects injected or via factory
        // - Required caller credentials
        // - Any callback context passed through from prior invocation

        // TODO: Remove this temporary logging
        this.logger.log(String.format("Invocation: %s", requestContext.getInvocation()));
        this.logger.log(request.getResourceModel().toString());

        // TODO: implement the handler invocation inside a time check which will abort and automatically
        // reschedule a callback if the handler does not respond within the 15 minute invocation window

        // TODO: ensure that any credential expiry time is also considered in the time check to
        // automatically fail a request if the handler will not be able to complete within that period,
        // such as before a FAS token expires

        final Date startTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        final ProgressEvent handlerResponse = invokeHandler(request, request.getAction(), requestContext);

        final Date endTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        metricsPublisher.publishDurationMetric(
            Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getAction(),
            (endTime.getTime() - startTime.getTime()));

        // When the handler responses InProgress with a callback delay, we trigger a callback to re-invoke
        // the handler for the Resource type to implement stabilization checks and long-poll creation checks
        if (handlerResponse.getStatus() == ProgressStatus.InProgress &&
            handlerResponse.getCallbackDelayMinutes() > 0) {
            // TODO: use CloudWatch events to re-invoke the handlers
        }
        
        // TODO: Implement callback to CloudFormation or specified callback API
        // to report the progress status when in non-terminal state (i.e; InProgress)

        // The wrapper will log any context to the configured CloudWatch log group
        if (handlerResponse.getCallbackContext() != null)
            this.logger.log(handlerResponse.getCallbackContext().toString());

        // A response will be output on all paths, though CloudFormation will
        // not block on invoking the handlers, but rather listen for callbacks
        writeResponse(outputStream, createProgressResponse(handlerResponse));
    }

    private Response createErrorResponse(final String errorMessage) {
        this.logger.log(errorMessage);
        return new Response(errorMessage);
    }

    private Response createProgressResponse(final ProgressEvent progressEvent) {
        this.logger.log(String.format("Got progress %s (%s)\n", progressEvent.getStatus(), progressEvent.getMessage()));
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

    /**
     * NOTE: This function needs to be updated to invoke the real interface/handlers
     */
    public ProgressEvent invokeHandler(final HandlerRequest<?> request,
                                       final Action action,
                                       final RequestContext context) {
        ProgressEvent progressEvent = null;
        final java.lang.reflect.Method method;
        try {
            final Class<?> handlerImpl = Class.forName("com.aws.cfn.GenericHandler");
            final Object handler = handlerImpl.newInstance();
            method = handlerImpl.getDeclaredMethod(
                "handleRequest",
                com.aws.rpdk.HandlerRequest.class,
                Action.class,
                com.aws.rpdk.RequestContext.class);
            progressEvent = (ProgressEvent) method.invoke(
                handler,
                request,
                action,
                context);
        }
        catch (final SecurityException e) {
            e.printStackTrace();
        }
        catch (final NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
        }
        catch (final InvocationTargetException e) {
            e.printStackTrace();
        }

        return progressEvent;
    }
}
