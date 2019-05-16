package com.aws.cfn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.aws.cfn.exceptions.TerminalException;
import com.aws.cfn.injection.CloudFormationProvider;
import com.aws.cfn.injection.CloudWatchEventsProvider;
import com.aws.cfn.injection.CloudWatchProvider;
import com.aws.cfn.injection.PlatformCredentialsProvider;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.metrics.MetricsPublisherImpl;
import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.CloudFormationCallbackAdapter;
import com.aws.cfn.proxy.Credentials;
import com.aws.cfn.proxy.HandlerErrorCode;
import com.aws.cfn.proxy.HandlerRequest;
import com.aws.cfn.proxy.OperationStatus;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.RequestContext;
import com.aws.cfn.proxy.ResourceHandlerRequest;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Serializer;
import com.aws.cfn.resource.Validator;
import com.aws.cfn.resource.exceptions.ValidationException;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public abstract class LambdaWrapper<ResourceT, CallbackT> implements RequestStreamHandler {

    private final PlatformCredentialsProvider platformCredentialsProvider = new PlatformCredentialsProvider();
    final CloudFormationProvider cloudFormationProvider = new CloudFormationProvider(this.platformCredentialsProvider);
    final CloudWatchProvider cloudWatchProvider = new CloudWatchProvider(this.platformCredentialsProvider);
    final CloudWatchEventsProvider cloudWatchEventsProvider = new CloudWatchEventsProvider(this.platformCredentialsProvider);

    private CallbackAdapter<ResourceT> callbackAdapter;
    private MetricsPublisher metricsPublisher;
    private CloudWatchScheduler scheduler;
    private final SchemaValidator validator;
    private final TypeReference<HandlerRequest<ResourceT, CallbackT>> typeReference;
    protected final Serializer serializer;
    protected LambdaLogger logger;

    protected LambdaWrapper() {
        this.serializer = new Serializer();
        this.validator = new Validator();
        this.typeReference = getTypeReference();
    }

    /**
     * This .ctor provided for testing
     */
    public LambdaWrapper(final CallbackAdapter<ResourceT> callbackAdapter,
                         final MetricsPublisher metricsPublisher,
                         final CloudWatchScheduler scheduler,
                         final SchemaValidator validator,
                         final Serializer serializer,
                         final TypeReference<HandlerRequest<ResourceT, CallbackT>> typeReference) {
        this.callbackAdapter = callbackAdapter;
        this.metricsPublisher = metricsPublisher;
        this.scheduler = scheduler;
        this.serializer = serializer;
        this.validator = validator;
        this.typeReference = typeReference;
    }

    /**
     * This function initialises dependencies which are depending on credentials passed
     * at function invoke and not available during construction
    */
    private void initialiseRuntime(final Credentials platformCredentials) {
        // initialisation skipped if these dependencies were set during injection (in test)
        this.platformCredentialsProvider.setCredentials(platformCredentials);
        if (this.callbackAdapter == null) {
            this.callbackAdapter = new CloudFormationCallbackAdapter<ResourceT>(this.cloudFormationProvider.get(), this.logger);
        }
        if (this.metricsPublisher == null) {
            this.metricsPublisher = new MetricsPublisherImpl(this.cloudWatchProvider.get());
        }
        if (this.scheduler == null) {
            this.scheduler = new CloudWatchScheduler(this.cloudWatchEventsProvider.get());
            this.scheduler.setLogger(this.logger);
        }
    }

    public void handleRequest(final InputStream inputStream,
                              final OutputStream outputStream,
                              final Context context) throws IOException, TerminalException {
        this.logger = context.getLogger();

        ProgressEvent<ResourceT, CallbackT> handlerResponse = null;
        HandlerRequest<ResourceT, CallbackT> request = null;

        try {
            if (inputStream == null) {
                throw new TerminalException("No request object received");
            }

            // decode the input request
            final String input = IOUtils.toString(inputStream, "UTF-8");
            request = this.serializer.deserialize(input, typeReference);
            handlerResponse = processInvocation(request, context);
        } catch (final Exception e) {
            // Exceptions are wrapped as a consistent error response to the caller (i.e; CloudFormation)
            e.printStackTrace(); // for root causing - logs to LambdaLogger by default

            this.metricsPublisher.publishExceptionMetric(
                Instant.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
                request.getAction(),
                e);

            handlerResponse = new ProgressEvent<>();
            handlerResponse.setMessage(e.getMessage());
            handlerResponse.setStatus(OperationStatus.FAILED);
            if (request.getRequestData() != null) {
                handlerResponse.setResourceModel(
                    request.getRequestData().getResourceProperties()
                );
            }
        } finally {
            // A response will be output on all paths, though CloudFormation will
            // not block on invoking the handlers, but rather listen for callbacks
            writeResponse(outputStream, createProgressResponse(handlerResponse, request.getBearerToken()));
        }
    }

    public ProgressEvent<ResourceT, CallbackT> processInvocation(final HandlerRequest<ResourceT, CallbackT> request,
                                           final Context context) throws IOException, TerminalException {

        if (request == null || request.getRequestData() == null) {
            throw new TerminalException("Invalid request object received");
        }

        // ensure required execution credentials have been passed and inject them
        if (request.getRequestData().getPlatformCredentials() == null) {
            throw new TerminalException("Missing required platform credentials");
        }

        // initialise dependencies with platform credentials
        initialiseRuntime(request.getRequestData().getPlatformCredentials());

        // transform the request object to pass to caller
        final ResourceHandlerRequest<ResourceT> resourceHandlerRequest = transform(request);

        final RequestContext<CallbackT> requestContext = request.getRequestContext();
        final boolean hasRequestContext = requestContext != null;
        final CallbackT callbackContext = hasRequestContext ? requestContext.getCallbackContext() : null;

        if (hasRequestContext) {
            // If this invocation was triggered by a 're-invoke' CloudWatch Event, clean it up
            final String cloudWatchEventsRuleName = requestContext.getCloudWatchEventsRuleName();
            if (!StringUtils.isBlank(cloudWatchEventsRuleName)) {
                this.scheduler.cleanupCloudWatchEvents(
                    cloudWatchEventsRuleName,
                    requestContext.getCloudWatchEventsTargetId());
            }
            logger.log(String.format("Cleaned up previous Request Context of Rule %s and Target %s", requestContext.getCloudWatchEventsRuleName(), requestContext.getCloudWatchEventsTargetId()));
        }

        // MetricsPublisher is initialised with the resource type name for metrics namespace
        this.metricsPublisher.setResourceTypeName(request.getResourceType());

        this.metricsPublisher.publishInvocationMetric(
            Instant.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getAction());

        // for CUD actions, validate incoming model - any error is a terminal failure on the invocation
        try {
            if (request.getAction() == Action.CREATE ||
                request.getAction() == Action.UPDATE ||
                request.getAction() == Action.DELETE) {
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

        // last mile proxy creation with passed-in credentials
        final AmazonWebServicesClientProxy awsClientProxy = new AmazonWebServicesClientProxy(
            this.logger,
            request.getRequestData().getCallerCredentials());

        final ProgressEvent<ResourceT, CallbackT> handlerResponse = invokeHandler(
            awsClientProxy,
            resourceHandlerRequest,
            request.getAction(),
            callbackContext);
        if (handlerResponse != null) {
            this.log(String.format("Handler returned %s", handlerResponse.getStatus()));
        } else {
            this.log("Handler returned null");
        }

        final Date endTime = Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        metricsPublisher.publishDurationMetric(
            Instant.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()),
            request.getAction(),
            (endTime.getTime() - startTime.getTime()));

        // ensure we got a valid response
        if (handlerResponse == null) {
            throw new TerminalException("Handler failed to provide a response.");
        }

        // When the handler responses IN_PROGRESS with a callback delay, we trigger a callback to re-invoke
        // the handler for the Resource type to implement stabilization checks and long-poll creation checks
        if (handlerResponse.getStatus() == OperationStatus.IN_PROGRESS) {
            final RequestContext<CallbackT> reinvocationContext = new RequestContext<>();

            int counter = 1;
            if (hasRequestContext) {
                counter += requestContext.getInvocation();
            }
            reinvocationContext.setInvocation(counter);

            reinvocationContext.setCallbackContext(handlerResponse.getCallbackContext());
            request.setRequestContext(reinvocationContext);

            logger.log(String.format("Scheduling re-invoke with Context {%s}", reinvocationContext.toString()));
            try {
                this.scheduler.rescheduleAfterMinutes(
                    context.getInvokedFunctionArn(),
                    handlerResponse.getCallbackDelayMinutes(),
                    request);
            } catch (Exception e){
                handlerResponse.setStatus(OperationStatus.FAILED);
                handlerResponse.setErrorCode(HandlerErrorCode.ServiceException);
            }
        }
        // report the progress status back to configured endpoint
        this.callbackAdapter.reportProgress(request.getBearerToken(),
                handlerResponse.getErrorCode(),
                handlerResponse.getStatus(),
                handlerResponse.getResourceModel(),
                handlerResponse.getMessage());

        return handlerResponse;
    }

    private Response<ResourceT> createProgressResponse(final ProgressEvent<ResourceT, CallbackT> progressEvent, final String bearerToken) {
        final Response<ResourceT> response = new Response<>();
        response.setMessage(progressEvent.getMessage());
        response.setOperationStatus(progressEvent.getStatus());
        response.setResourceModel(progressEvent.getResourceModel());
        response.setBearerToken(bearerToken);
        response.setErrorCode(progressEvent.getErrorCode());
        return response;
    }

    private void writeResponse(final OutputStream outputStream,
                               final Response<ResourceT> response) throws IOException {

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

        this.validator.validateObject(modelObject, resourceSchema);
    }

    /**
     * Transforms the incoming request to the subset of typed models which the handler implementor needs
     * @param request   The request as passed from the caller (e.g; CloudFormation) which contains
     *                  additional context to inform the LambdaWrapper itself, and is not needed by the
     *                  handler implementations
     * @return  A converted ResourceHandlerRequest model
     */
    protected abstract ResourceHandlerRequest<ResourceT> transform(final HandlerRequest<ResourceT, CallbackT> request) throws IOException;

    /**
     * Handler implementation should implement this method to provide the schema for validation
     * @return  An InputStream of the resource schema for the provider
     */
    protected abstract InputStream provideResourceSchema();

    /**
     * Implemented by the handler package as the key entry point.
     */
    public abstract ProgressEvent<ResourceT, CallbackT> invokeHandler(final AmazonWebServicesClientProxy proxy,
                                                   final ResourceHandlerRequest<ResourceT> request,
                                                   final Action action,
                                                   final CallbackT callbackContext) throws IOException;

    /**
     * null-safe logger redirect
     * @param message A string containing the event to log.
     */
    private void log(final String message) {
        if (this.logger != null) {
            this.logger.log(String.format("%s\n", message));
        }
    }

    protected abstract TypeReference<HandlerRequest<ResourceT, CallbackT>> getTypeReference();
}
