package com.aws.cfn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.aws.cfn.exceptions.TerminalException;
import com.aws.cfn.injection.LambdaModule;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.HandlerRequest;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.ProgressStatus;
import com.aws.cfn.proxy.RequestContext;
import com.aws.cfn.proxy.ResourceHandlerRequest;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Serializer;
import com.aws.cfn.resource.exceptions.ValidationException;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public abstract class LambdaWrapper<T> implements RequestStreamHandler {

    private final CallbackAdapter callbackAdapter;
    private final MetricsPublisher metricsPublisher;
    private final CloudWatchScheduler scheduler;
    private final SchemaValidator validator;
    protected final Serializer serializer;
    protected LambdaLogger logger;

    /**
     * This .ctor provided for Lambda runtime which will not automatically invoke Guice injector
     */
    public LambdaWrapper() {
        final Injector injector = Guice.createInjector(new LambdaModule());
        this.callbackAdapter = injector.getInstance(CallbackAdapter.class);
        this.metricsPublisher = injector.getInstance(MetricsPublisher.class);
        this.scheduler = new CloudWatchScheduler();
        this.serializer = new Serializer();
        this.validator = injector.getInstance(SchemaValidator.class);
    }

    /**
     * This .ctor provided for testing
     */
    @Inject
    public LambdaWrapper(final CallbackAdapter callbackAdapter,
                         final MetricsPublisher metricsPublisher,
                         final CloudWatchScheduler scheduler,
                         final SchemaValidator validator,
                         final Serializer serializer) {
        this.callbackAdapter = callbackAdapter;
        this.metricsPublisher = metricsPublisher;
        this.scheduler = scheduler;
        this.serializer = serializer;
        this.validator = validator;
    }

    public void handleRequest(final InputStream inputStream,
                              final OutputStream outputStream,
                              final Context context) throws IOException, TerminalException {
        this.logger = context.getLogger();
        this.scheduler.setLogger(context.getLogger());

        ProgressEvent handlerResponse = null;
        HandlerRequest request = null;

        try {
            if (inputStream == null) {
                throw new TerminalException("No request object received");
            }

            // decode the input request
            final String input = IOUtils.toString(inputStream, "UTF-8");
            request = this.serializer.deserialize(input, HandlerRequest.class);

            handlerResponse = processInvocation(request, context);
        } catch (final Exception e) {
            // Exceptions are wrapped as a consistent error response to the caller (i.e; CloudFormation)
            e.printStackTrace(); // for root causing - logs to LambdaLogger by default

            this.metricsPublisher.publishExceptionMetric(
                Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
                request.getAction(),
                e);

            handlerResponse = new ProgressEvent();
            handlerResponse.setMessage(e.getMessage());
            handlerResponse.setStatus(ProgressStatus.Failed);
            if (request.getRequestData() != null) {
                handlerResponse.setResourceModel(
                    request.getRequestData().getResourceProperties()
                );
            }
        } finally {
            // A response will be output on all paths, though CloudFormation will
            // not block on invoking the handlers, but rather listen for callbacks
            writeResponse(outputStream, createProgressResponse(handlerResponse));
        }
    }

    public ProgressEvent processInvocation(final HandlerRequest request,
                                           final Context context) throws IOException, TerminalException {

        if (request == null || request.getRequestContext() == null) {
            throw new TerminalException("Invalid request object received");
        }

        // transform the request object to pass to caller
        final ResourceHandlerRequest resourceHandlerRequest = transform(request);

        final RequestContext requestContext = request.getRequestContext();

        // If this invocation was triggered by a 're-invoke' CloudWatch Event, clean it up
        if (requestContext.getCloudWatchEventsRuleName() != null &&
            !requestContext.getCloudWatchEventsRuleName().isEmpty()) {
            this.scheduler.cleanupCloudWatchEvents(
                requestContext.getCloudWatchEventsRuleName(),
                requestContext.getCloudWatchEventsTargetId());
        }

        // MetricsPublisher is initialised with the resource type name for metrics namespace
        this.metricsPublisher.setResourceTypeName(request.getResourceType());

        this.metricsPublisher.publishInvocationMetric(
            Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getAction());

        // for CUD actions, validate incoming model - any error is a terminal failure on the invocation
        try {
            if (request.getAction() == Action.Create ||
                request.getAction() == Action.Update ||
                request.getAction() == Action.Delete) {
                validateModel(this.serializer.serialize(resourceHandlerRequest.getDesiredResourceState()));
            }
        } catch (final ValidationException e) {
            // TODO: we'll need a better way to expose the stack of causing exceptions for user feedback
            final StringBuilder validationMessageBuilder = new StringBuilder();
            validationMessageBuilder.append(String.format("Model validation failed (%s)\n", e.getMessage()));
            for (final ValidationException cause : e.getCausingExceptions()) {
                validationMessageBuilder.append(String.format("%s (%s)",
                    cause.getMessage(),
                    cause.getSchemaLocation()));
            }
            throw new TerminalException(validationMessageBuilder.toString(), e);
        }

        // TODO: implement decryption of request and returned callback context
        // using KMS Key accessible by the Lambda execution Role

        // TODO: implement the handler invocation inside a time check which will abort and automatically
        // reschedule a callback if the handler does not respond within the 15 minute invocation window

        // TODO: ensure that any credential expiry time is also considered in the time check to
        // automatically fail a request if the handler will not be able to complete within that period,
        // such as before a FAS token expires

        final Date startTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        final ProgressEvent handlerResponse = invokeHandler(
            resourceHandlerRequest,
            request.getAction(),
            requestContext);
        if (handlerResponse != null)
            this.log(String.format("Handler returned %s", handlerResponse.getStatus()));
        else
            this.log("Handler returned null");

        final Date endTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        metricsPublisher.publishDurationMetric(
            Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getAction(),
            (endTime.getTime() - startTime.getTime()));

        // ensure we got a valid response
        if (handlerResponse == null) {
            throw new TerminalException("Handler failed to provide a response.");
        }

        // When the handler responses InProgress with a callback delay, we trigger a callback to re-invoke
        // the handler for the Resource type to implement stabilization checks and long-poll creation checks
        if (handlerResponse.getStatus() == ProgressStatus.InProgress) {
            final RequestContext callbackContext = new RequestContext();
            callbackContext.setInvocation(requestContext.getInvocation() + 1);
            callbackContext.setCallbackContext(handlerResponse.getCallbackContext());

            this.scheduler.rescheduleAfterMinutes(
                context.getInvokedFunctionArn(),
                handlerResponse.getCallbackDelayMinutes(),
                callbackContext);

            // report the progress status when in non-terminal state (i.e; InProgress) back to configured endpoint
            this.callbackAdapter.reportProgress(request.getBearerToken(),
                handlerResponse.getErrorCode(),
                handlerResponse.getStatus(),
                handlerResponse.getResourceModel(),
                handlerResponse.getMessage());
        }

        // The wrapper will log any context to the configured CloudWatch log group
        if (handlerResponse.getCallbackContext() != null)
            this.log(handlerResponse.getCallbackContext().toString());

        return handlerResponse;
    }

    private Response<T> createProgressResponse(final ProgressEvent<T> progressEvent) throws JsonProcessingException {
        final Response<T> response = new Response<>();
        response.setMessage(progressEvent.getMessage());
        response.setStatus(progressEvent.getStatus());

        if (progressEvent.getResourceModel() != null) {
            response.setResourceModel(progressEvent.getResourceModel());
        }

        return response;
    }

    private void writeResponse(final OutputStream outputStream,
                               final Response response) throws IOException {

        final JSONObject output = this.serializer.serialize(response);
        outputStream.write(output.toString().getBytes(Charset.forName("UTF-8")));
        outputStream.close();
    }

    private void validateModel(final JSONObject modelObject) throws ValidationException {
        final InputStream resourceSchema = provideResourceSchema();
        if (resourceSchema == null) {
            throw new ValidationException(
                "Unable to validate incoming model as no schema was provided.",
                null,
                null);
        }

        this.validator.validateModel(modelObject, resourceSchema);
    }

    /**
     * Transforms the incoming request to the subset of typed models which the handler implementor needs
     * @param request   The request as passed from the caller (e.g; CloudFormation) which contains
     *                  additional contex to inform the LambdaWrapper itself, and is not needed by the
     *                  handler implementations
     * @return  A converted ResourceHandlerRequest model
     */
    protected abstract ResourceHandlerRequest<T> transform(final HandlerRequest request) throws IOException;

    /**
     * Handler implementation should implement this method to provide the schema for validation
     * @return  An InputStream of the resource schema for the provider
     */
    protected abstract InputStream provideResourceSchema();

    /**
     * Implemented by the handler package as the key entry point.
     */
    public abstract ProgressEvent<T> invokeHandler(final ResourceHandlerRequest<T> request,
                                                   final Action action,
                                                   final RequestContext context) throws IOException;

    /**
     * null-safe logger redirect
     * @param message A string containing the event to log.
     */
    private void log(final String message) {
        if (this.logger != null) {
            this.logger.log(String.format("%s\n", message));
        }
    }
}
