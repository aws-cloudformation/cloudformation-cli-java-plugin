package com.aws.cfn.oasis;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.aws.cfn.Action;
import com.aws.cfn.exceptions.TerminalException;
import com.aws.cfn.injection.LambdaModule;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.oasis.model.IterationProviderHandlerRequest;
import com.aws.cfn.oasis.model.IterationProviderWrapperRequest;
import com.aws.cfn.oasis.model.ProgressEvent;
import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.oasis.model.OperationInfo;
import com.aws.cfn.oasis.model.iteration.Iteration;
import com.aws.cfn.oasis.model.nextaction.NextActionFail;
import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.HandlerErrorCode;
import com.aws.cfn.proxy.OperationStatus;
import com.aws.cfn.proxy.RequestContext;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Serializer;
import com.aws.cfn.resource.exceptions.ValidationException;
import com.aws.cfn.oasis.scheduler.CloudWatchScheduler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import software.amazon.awssdk.utils.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the translation from request from the CloudFormation service to invoking the
 * handler itself
 */
public abstract class LambdaWrapper<InputT, CallbackT> implements RequestStreamHandler {

    private final CallbackAdapter<Collection<Iteration>> callbackAdapter;
    private final MetricsPublisher metricsPublisher;
    private final CloudWatchScheduler scheduler;
    private final SchemaValidator validator;
    private final TypeReference<IterationProviderWrapperRequest<InputT, CallbackT>> inputTypeReference;
    protected final Serializer serializer;
    protected LambdaLogger logger;


    /**
     * This .ctor provided for Lambda runtime which will not automatically invoke Guice injector
     */
    @SuppressWarnings("unused")
    public LambdaWrapper() {
        Injector injector = Guice.createInjector(new LambdaModule());
        this.callbackAdapter = getCallbackAdapter();
        this.metricsPublisher = injector.getInstance(MetricsPublisher.class);
        this.scheduler = new CloudWatchScheduler();
        this.serializer = new Serializer();
        this.validator = injector.getInstance(SchemaValidator.class);
        this.inputTypeReference = getInputTypeReference();
    }

    /**
     * This .ctor provided for testing
     */
    @Inject
    @VisibleForTesting
    public LambdaWrapper(@Nonnull final CallbackAdapter<Collection<Iteration>> callbackAdapter,
                         @Nonnull final MetricsPublisher metricsPublisher,
                         @Nonnull final CloudWatchScheduler scheduler,
                         @Nonnull final SchemaValidator validator,
                         @Nonnull final Serializer serializer,
                         @Nonnull final TypeReference<IterationProviderWrapperRequest<InputT, CallbackT>> inputTypeReference) {
        this.callbackAdapter = Objects.requireNonNull(callbackAdapter);
        this.metricsPublisher = Objects.requireNonNull(metricsPublisher);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.serializer = Objects.requireNonNull(serializer);
        this.validator = Objects.requireNonNull(validator);
        this.inputTypeReference = Objects.requireNonNull(inputTypeReference);
    }

    public void handleRequest(@Nonnull final InputStream inputStream,
                              @Nonnull final OutputStream outputStream,
                              @Nonnull final Context context) throws IOException, TerminalException {
        Objects.requireNonNull(inputStream);
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(context);

        ProgressEvent<CallbackT> handlerResponse = null;
        IterationProviderWrapperRequest<InputT, CallbackT> request = null;

        try {
            this.logger = context.getLogger();
            this.scheduler.setLogger(context.getLogger());

            // decode the input request
            final String input = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            request = this.serializer.deserialize(input, inputTypeReference);
            handlerResponse = processInvocation(request, context);
        } catch (final Exception ex) {
            // Exceptions are wrapped as a consistent error response to the caller (i.e; CloudFormation)
            ex.printStackTrace(); // for root causing - logs to LambdaLogger by default

            handlerResponse = new ProgressEvent<>(new NextActionFail<>(ex.getMessage(), HandlerErrorCode.InternalFailure));

            final Action action = Optional.ofNullable(request)
                                          .map(IterationProviderWrapperRequest::getOperationInfo)
                                          .map(OperationInfo::getAction)
                                          .orElse(null);

            // this.metricsPublisher.publishExceptionMetric(
            //         Instant.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            //         action,
            //         ex
            // );
        } finally {
            if (request != null && handlerResponse != null) {
                // A response will be output on all paths, though CloudFormation will
                // not block on invoking the handlers, but rather listen for callbacks
                writeResponse(outputStream, createProgressResponse(
                        handlerResponse,
                        request.getBearerToken()
                ));
            }
        }
    }

    public ProgressEvent<CallbackT> processInvocation(@Nonnull final IterationProviderWrapperRequest<InputT, CallbackT> request,
                                                      @Nonnull final Context context) throws IOException, TerminalException {

        if (request == null) throw new TerminalException("Invalid request object received");
        if (context == null) throw new TerminalException("Invalid context object received");

        // transform the request object to pass to caller
        final IterationProviderHandlerRequest<InputT> handlerRequest = transform(request);

        final Optional<RequestContext<CallbackT>> requestContext = Optional.ofNullable(request.getRequestContext());

        requestContext.ifPresent(rc -> {
            // If this invocation was triggered by a 're-invoke' CloudWatch Event, clean it up
            final String cloudWatchEventsRuleName = rc.getCloudWatchEventsRuleName();
            if (!StringUtils.isBlank(cloudWatchEventsRuleName)) {
                this.scheduler.cleanupCloudWatchEvents(
                        cloudWatchEventsRuleName,
                        rc.getCloudWatchEventsTargetId()
                );
            }
        });

        // MetricsPublisher is initialised with the resource type name for metrics namespace
        // TODO fixup the metrics story for hooks
        // this.metricsPublisher.publishInvocationMetric(
        //         Instant.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
        //         request.getOperationInfo().getAction()
        // );

        // for CUD actions, validate incoming model - any error is a terminal failure on the invocation
        try {
            validateModel(this.serializer.serialize(handlerRequest.getConfiguration()));
        } catch (final ValidationException e) {
            // TODO: we'll need a better way to expose the stack of causing exceptions for user feedback
            final StringBuilder validationMessageBuilder = new StringBuilder();
            validationMessageBuilder.append(String.format("Model validation failed (%s)\n", e.getMessage()));
            for (final ValidationException cause : e.getCausingExceptions()) {
                validationMessageBuilder.append(String.format(
                        "%s (%s)",
                        cause.getMessage(),
                        cause.getSchemaLocation()
                ));
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

        // last mile proxy creation with passed-in credentials
        final AmazonWebServicesClientProxy awsClientProxy = new AmazonWebServicesClientProxy(
                this.logger,
                request.getCredentials()
        );

        final ProgressEvent<CallbackT> handlerResponse = invokeHandler(
                awsClientProxy,
                handlerRequest,
                request.getOperationInfo().getAction(),
                requestContext.map(RequestContext::getCallbackContext).orElse(null)
        );

        if (handlerResponse != null) {
            this.log(String.format("Handler returned %s", handlerResponse.getNextAction().getOperationStatus()));
        } else {
            this.log("Handler returned null");
        }

        final Date endTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        // metricsPublisher.publishDurationMetric(
        //         Instant.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
        //         request.getOperationInfo().getAction(),
        //         (endTime.getTime() - startTime.getTime())
        // );

        // ensure we got a valid response
        if (handlerResponse == null) {
            throw new TerminalException("Handler failed to provide a response.");
        }

        final Response<CallbackT> response = createProgressResponse(
                handlerResponse,
                request.getBearerToken()
        );

        // When the handler responses IN_PROGRESS with a callback delay, we trigger a callback to re-invoke
        // the handler for the Resource type to implement stabilization checks and long-poll creation checks
        if (handlerResponse.getNextAction().getOperationStatus() == OperationStatus.IN_PROGRESS) {
            final RequestContext<CallbackT> reinvocationContext = new RequestContext<>();

            int counter = 1;
            counter += requestContext.map(RequestContext::getInvocation).orElse(0);
            reinvocationContext.setInvocation(counter);

            reinvocationContext.setCallbackContext(response.getCallbackContext());
            final IterationProviderWrapperRequest<InputT, CallbackT> reinvocationRequest = request.toBuilder().requestContext(reinvocationContext).build();

            this.scheduler.rescheduleAfterMinutes(
                    context.getInvokedFunctionArn(),
                    response.getCallbackDelayMinutes(),
                    reinvocationRequest
            );
        }

        // report the progress status back to configured endpoint
        this.callbackAdapter.reportProgress(
                response.getBearerToken(),
                response.getErrorCode(),
                response.getOperationStatus(),
                response.getOutputModel(),
                response.getMessage()
        );

        return handlerResponse;
    }

    private Response<CallbackT> createProgressResponse(@Nonnull final ProgressEvent<CallbackT> progressEvent,
                                                       @Nonnull final String bearerToken) {
        Objects.requireNonNull(progressEvent);
        Objects.requireNonNull(bearerToken);

        final Response<CallbackT> response = new Response<>();
        response.setOperationStatus(progressEvent.getNextAction().getOperationStatus());
        progressEvent.getNextAction().decorateResponse(response);
        response.setBearerToken(bearerToken);
        return response;
    }

    private void writeResponse(final OutputStream outputStream,
                               final Response<CallbackT> response) throws IOException {

        final JSONObject output = this.serializer.serialize(response);
        outputStream.write(output.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.close();
    }

    private void validateModel(final JSONObject modelObject) throws ValidationException {
        final InputStream resourceSchema = provideSchema();
        if (resourceSchema == null) {
            throw new ValidationException(
                    "Unable to validate incoming model as no schema was provided.",
                    null,
                    null
            );
        }

        this.validator.validateObject(modelObject, resourceSchema);
    }

    /**
     * Transforms the incoming request to the subset of typed models which the handler implementor needs
     *
     * @param request The request as passed from the caller (e.g; CloudFormation) which contains
     *                additional context to inform the LambdaWrapper itself, and is not needed by the
     *                handler implementations
     * @return A converted ResourceHandlerRequest model
     */
    protected IterationProviderHandlerRequest<InputT> transform(@Nonnull final IterationProviderWrapperRequest<InputT, CallbackT> request) throws IOException {
        return new IterationProviderHandlerRequest<>(
                request.getOperationInfo(),
                request.getBearerToken(),
                request.getInputConfiguration(),
                request.getTemplates()
        );
    }

    /**
     * Handler implementation should implement this method to provide the schema for validation
     *
     * @return An InputStream of the resource schema for the provider
     */
    protected abstract InputStream provideSchema();

    /**
     * Implemented by the handler package as the key entry point.
     */
    public abstract ProgressEvent<CallbackT> invokeHandler(final AmazonWebServicesClientProxy proxy,
                                                           final IterationProviderHandlerRequest<InputT> request,
                                                           final Action action,
                                                           final CallbackT callbackContext) throws IOException;

    /**
     * null-safe logger redirect
     *
     * @param message A string containing the event to log.
     */
    private void log(final String message) {
        if (this.logger != null) {
            this.logger.log(String.format("%s\n", message));
        }
    }

    protected abstract CallbackAdapter<Collection<Iteration>> getCallbackAdapter();

    protected abstract TypeReference<IterationProviderWrapperRequest<InputT, CallbackT>> getInputTypeReference();
}
