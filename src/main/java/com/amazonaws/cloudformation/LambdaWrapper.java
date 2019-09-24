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
package com.amazonaws.cloudformation;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.cloudformation.exceptions.FileScrubberException;
import com.amazonaws.cloudformation.exceptions.ResourceAlreadyExistsException;
import com.amazonaws.cloudformation.exceptions.ResourceNotFoundException;
import com.amazonaws.cloudformation.exceptions.TerminalException;
import com.amazonaws.cloudformation.injection.CloudFormationProvider;
import com.amazonaws.cloudformation.injection.CloudWatchEventsProvider;
import com.amazonaws.cloudformation.injection.CloudWatchLogsProvider;
import com.amazonaws.cloudformation.injection.CloudWatchProvider;
import com.amazonaws.cloudformation.injection.CredentialsProvider;
import com.amazonaws.cloudformation.injection.SessionCredentialsProvider;
import com.amazonaws.cloudformation.loggers.CloudWatchLogHelper;
import com.amazonaws.cloudformation.loggers.CloudWatchLogPublisher;
import com.amazonaws.cloudformation.loggers.LambdaLogPublisher;
import com.amazonaws.cloudformation.loggers.LogPublisher;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;
import com.amazonaws.cloudformation.metrics.MetricsPublisherImpl;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.CallbackAdapter;
import com.amazonaws.cloudformation.proxy.CloudFormationCallbackAdapter;
import com.amazonaws.cloudformation.proxy.Credentials;
import com.amazonaws.cloudformation.proxy.HandlerErrorCode;
import com.amazonaws.cloudformation.proxy.HandlerRequest;
import com.amazonaws.cloudformation.proxy.LoggerProxy;
import com.amazonaws.cloudformation.proxy.MetricsPublisherProxy;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.RequestContext;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.resource.SchemaValidator;
import com.amazonaws.cloudformation.resource.Serializer;
import com.amazonaws.cloudformation.resource.Validator;
import com.amazonaws.cloudformation.resource.exceptions.ValidationException;
import com.amazonaws.cloudformation.scheduler.CloudWatchScheduler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import software.amazon.awssdk.services.cloudformation.model.OperationStatusCheckFailedException;
import software.amazon.awssdk.utils.StringUtils;

public abstract class LambdaWrapper<ResourceT, CallbackT> implements RequestStreamHandler {

    private static final List<Action> MUTATING_ACTIONS = Arrays.asList(Action.CREATE, Action.DELETE, Action.UPDATE);

    protected final Serializer serializer;
    protected LoggerProxy loggerProxy;
    protected MetricsPublisherProxy metricsPublisherProxy;

    // Keep lambda logger as the last fallback log delivery approach
    protected LambdaLogger lambdaLogger;

    // provider... prefix indicates credential provided by resource owner

    private final CredentialsProvider platformCredentialsProvider;
    private final CredentialsProvider providerCredentialsProvider;

    private final CloudFormationProvider cloudFormationProvider;
    private final CloudWatchProvider platformCloudWatchProvider;
    private final CloudWatchProvider providerCloudWatchProvider;
    private final CloudWatchEventsProvider platformCloudWatchEventsProvider;
    private final CloudWatchLogsProvider cloudWatchLogsProvider;
    private final SchemaValidator validator;
    private final TypeReference<HandlerRequest<ResourceT, CallbackT>> typeReference;

    private CallbackAdapter<ResourceT> callbackAdapter;
    private MetricsPublisher platformMetricsPublisher;
    private MetricsPublisher providerMetricsPublisher;
    private CloudWatchScheduler scheduler;

    private LogPublisher platformLambdaLogger;
    private CloudWatchLogHelper cloudWatchLogHelper;
    private CloudWatchLogPublisher providerEventsLogger;

    protected LambdaWrapper() {
        this.platformCredentialsProvider = new SessionCredentialsProvider();
        this.providerCredentialsProvider = new SessionCredentialsProvider();
        this.cloudFormationProvider = new CloudFormationProvider(this.platformCredentialsProvider);
        this.platformCloudWatchProvider = new CloudWatchProvider(this.platformCredentialsProvider);
        this.providerCloudWatchProvider = new CloudWatchProvider(this.providerCredentialsProvider);
        this.platformCloudWatchEventsProvider = new CloudWatchEventsProvider(this.platformCredentialsProvider);
        this.cloudWatchLogsProvider = new CloudWatchLogsProvider(this.providerCredentialsProvider);
        this.serializer = new Serializer();
        this.validator = new Validator();
        this.typeReference = getTypeReference();
    }

    /**
     * This .ctor provided for testing
     */
    public LambdaWrapper(final CallbackAdapter<ResourceT> callbackAdapter,
                         final CredentialsProvider platformCredentialsProvider,
                         final CredentialsProvider providerCredentialsProvider,
                         final CloudWatchLogPublisher providerEventsLogger,
                         final LogPublisher platformEventsLogger,
                         final MetricsPublisher platformMetricsPublisher,
                         final MetricsPublisher providerMetricsPublisher,
                         final CloudWatchScheduler scheduler,
                         final SchemaValidator validator,
                         final Serializer serializer) {

        this.callbackAdapter = callbackAdapter;
        this.platformCredentialsProvider = platformCredentialsProvider;
        this.providerCredentialsProvider = providerCredentialsProvider;
        this.cloudFormationProvider = new CloudFormationProvider(this.platformCredentialsProvider);
        this.platformCloudWatchProvider = new CloudWatchProvider(this.platformCredentialsProvider);
        this.providerCloudWatchProvider = new CloudWatchProvider(this.providerCredentialsProvider);
        this.platformCloudWatchEventsProvider = new CloudWatchEventsProvider(this.platformCredentialsProvider);
        this.cloudWatchLogsProvider = new CloudWatchLogsProvider(this.providerCredentialsProvider);
        this.providerEventsLogger = providerEventsLogger;
        this.platformLambdaLogger = platformEventsLogger;
        this.platformMetricsPublisher = platformMetricsPublisher;
        this.providerMetricsPublisher = providerMetricsPublisher;
        this.scheduler = scheduler;
        this.serializer = serializer;
        this.validator = validator;
        this.typeReference = getTypeReference();
    }

    /**
     * This function initialises dependencies which are depending on credentials
     * passed at function invoke and not available during construction
     */
    private void initialiseRuntime(final String resourceType,
                                   final Credentials platformCredentials,
                                   final Credentials providerCredentials,
                                   final String providerLogGroupName,
                                   final Context context,
                                   final String awsAccountId,
                                   final URI callbackEndpoint) {

        this.loggerProxy = new LoggerProxy();
        this.metricsPublisherProxy = new MetricsPublisherProxy();

        this.platformLambdaLogger = new LambdaLogPublisher(context.getLogger());
        this.loggerProxy.addLogPublisher(this.platformLambdaLogger);

        this.cloudFormationProvider.setCallbackEndpoint(callbackEndpoint);
        this.platformCredentialsProvider.setCredentials(platformCredentials);

        // Initialisation skipped if dependencies were set during injection (in unit
        // tests).
        // e.g. "if (this.platformMetricsPublisher == null)"
        if (this.platformMetricsPublisher == null) {
            // platformMetricsPublisher needs aws account id to differentiate metrics
            // namespace
            this.platformMetricsPublisher = new MetricsPublisherImpl(this.platformCloudWatchProvider, this.loggerProxy,
                                                                     awsAccountId, resourceType);
        }
        this.metricsPublisherProxy.addMetricsPublisher(this.platformMetricsPublisher);
        this.platformMetricsPublisher.refreshClient();

        // NOTE: providerCredentials and providerLogGroupName are null/not null in
        // sync.
        // Both are required parameters when LoggingConfig (optional) is provided when
        // 'RegisterType'.
        if (providerCredentials != null) {
            if (this.providerCredentialsProvider != null) {
                this.providerCredentialsProvider.setCredentials(providerCredentials);
            }

            if (this.providerMetricsPublisher == null) {
                this.providerMetricsPublisher = new MetricsPublisherImpl(this.providerCloudWatchProvider, this.loggerProxy,
                                                                         awsAccountId, resourceType);
            }
            this.metricsPublisherProxy.addMetricsPublisher(this.providerMetricsPublisher);
            this.providerMetricsPublisher.refreshClient();

            if (this.providerEventsLogger == null) {
                this.cloudWatchLogHelper = new CloudWatchLogHelper(this.cloudWatchLogsProvider, providerLogGroupName,
                                                                   context.getLogger(), this.metricsPublisherProxy);
                this.cloudWatchLogHelper.refreshClient();

                this.providerEventsLogger = new CloudWatchLogPublisher(this.cloudWatchLogsProvider, providerLogGroupName,
                                                                       this.cloudWatchLogHelper.prepareLogStream(),
                                                                       context.getLogger(), this.metricsPublisherProxy);
            }
            this.loggerProxy.addLogPublisher(this.providerEventsLogger);
            this.providerEventsLogger.refreshClient();
        }

        if (this.callbackAdapter == null) {
            this.callbackAdapter = new CloudFormationCallbackAdapter<ResourceT>(this.cloudFormationProvider, this.loggerProxy,
                                                                                this.serializer);
        }
        this.callbackAdapter.refreshClient();

        if (this.scheduler == null) {
            this.scheduler = new CloudWatchScheduler(this.platformCloudWatchEventsProvider, this.loggerProxy, this.serializer);
        }
        this.scheduler.refreshClient();
    }

    @Override
    public void handleRequest(final InputStream inputStream, final OutputStream outputStream, final Context context)
        throws IOException,
        TerminalException {

        this.lambdaLogger = context.getLogger();
        ProgressEvent<ResourceT, CallbackT> handlerResponse = null;
        HandlerRequest<ResourceT, CallbackT> request = null;
        scrubFiles();
        try {
            if (inputStream == null) {
                throw new TerminalException("No request object received");
            }

            String input = IOUtils.toString(inputStream, "UTF-8");
            JSONObject rawInput = new JSONObject(new JSONTokener(input));

            // deserialize incoming payload to modelled request
            request = this.serializer.deserialize(input, typeReference);
            handlerResponse = processInvocation(rawInput, request, context);
        } catch (final ValidationException e) {
            String message;
            String fullExceptionMessage = ValidationException.buildFullExceptionMessage(e);
            if (!StringUtils.isEmpty(fullExceptionMessage)) {
                message = String.format("Model validation failed (%s)", fullExceptionMessage);
            } else {
                message = "Model validation failed with unknown cause.";
            }

            publishExceptionMetric(request == null ? null : request.getAction(), e, HandlerErrorCode.InvalidRequest);
            handlerResponse = ProgressEvent.defaultFailureHandler(new TerminalException(message, e),
                HandlerErrorCode.InvalidRequest);
        } catch (final Throwable e) {
            // Exceptions are wrapped as a consistent error response to the caller (i.e;
            // CloudFormation)
            e.printStackTrace(); // for root causing - logs to LambdaLogger by default
            handlerResponse = ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InternalFailure);
            if (request != null && request.getRequestData() != null && MUTATING_ACTIONS.contains(request.getAction())) {
                handlerResponse.setResourceModel(request.getRequestData().getResourceProperties());
            }
            if (request != null) {
                publishExceptionMetric(request.getAction(), e, HandlerErrorCode.InternalFailure);
            }

        } finally {
            // A response will be output on all paths, though CloudFormation will
            // not block on invoking the handlers, but rather listen for callbacks
            writeResponse(outputStream,
                createProgressResponse(handlerResponse, request != null ? request.getBearerToken() : null));
        }
    }

    private ProgressEvent<ResourceT, CallbackT>
        processInvocation(final JSONObject rawRequest, final HandlerRequest<ResourceT, CallbackT> request, final Context context)
            throws IOException,
            TerminalException {

        assert request != null : "Invalid request object received";

        if (request.getRequestData() == null) {
            throw new TerminalException("Invalid request object received");
        }

        if (MUTATING_ACTIONS.contains(request.getAction())) {
            if (request.getRequestData().getResourceProperties() == null) {
                throw new TerminalException("Invalid resource properties object received");
            }
        }

        if (StringUtils.isEmpty(request.getResponseEndpoint())) {
            throw new TerminalException("No callback endpoint received");
        }

        // ensure required execution credentials have been passed and inject them
        if (request.getRequestData().getPlatformCredentials() == null) {
            throw new TerminalException("Missing required platform credentials");
        }

        // initialise dependencies with platform credentials
        initialiseRuntime(request.getResourceType(), request.getRequestData().getPlatformCredentials(),
            request.getRequestData().getProviderCredentials(), request.getRequestData().getProviderLogGroupName(), context,
            request.getAwsAccountId(), URI.create(request.getResponseEndpoint()));

        // transform the request object to pass to caller
        ResourceHandlerRequest<ResourceT> resourceHandlerRequest = transform(request);

        RequestContext<CallbackT> requestContext = request.getRequestContext();

        if (requestContext == null || requestContext.getInvocation() == 0) {
            // Acknowledge the task for first time invocation
            this.callbackAdapter.reportProgress(request.getBearerToken(), null, OperationStatus.IN_PROGRESS,
                OperationStatus.PENDING, null, null);
        }

        if (requestContext != null) {
            // If this invocation was triggered by a 're-invoke' CloudWatch Event, clean it
            // up
            String cloudWatchEventsRuleName = requestContext.getCloudWatchEventsRuleName();
            if (!StringUtils.isBlank(cloudWatchEventsRuleName)) {
                this.scheduler.cleanupCloudWatchEvents(cloudWatchEventsRuleName, requestContext.getCloudWatchEventsTargetId());
                log(String.format("Cleaned up previous Request Context of Rule %s and Target %s",
                    requestContext.getCloudWatchEventsRuleName(), requestContext.getCloudWatchEventsTargetId()));
            }
        }

        this.metricsPublisherProxy.publishInvocationMetric(Instant.now(), request.getAction());

        // for CUD actions, validate incoming model - any error is a terminal failure on
        // the invocation
        // NOTE: we validate the raw pre-deserialized payload to account for lenient
        // serialization.
        // Here, we want to surface ALL input validation errors to the caller.
        boolean isMutatingAction = MUTATING_ACTIONS.contains(request.getAction());
        if (isMutatingAction) {
            // validate entire incoming payload, including extraneous fields which
            // are stripped by the Serializer (due to FAIL_ON_UNKNOWN_PROPERTIES setting)
            JSONObject rawModelObject = rawRequest.getJSONObject("requestData").getJSONObject("resourceProperties");
            try {
                validateModel(rawModelObject);
            } catch (final ValidationException e) {
                // TODO: we'll need a better way to expose the stack of causing exceptions for
                // user feedback
                StringBuilder validationMessageBuilder = new StringBuilder();
                if (!StringUtils.isEmpty(e.getMessage())) {
                    validationMessageBuilder.append(String.format("Model validation failed (%s)", e.getMessage()));
                } else {
                    validationMessageBuilder.append("Model validation failed with unknown cause.");
                }
                List<ValidationException> es = e.getCausingExceptions();
                if (CollectionUtils.isNotEmpty(es)) {
                    for (RuntimeException cause : es) {
                        if (cause instanceof ValidationException) {
                            validationMessageBuilder.append(String.format("%n%s (%s)", cause.getMessage(),
                                ((ValidationException) cause).getSchemaLocation()));
                        }
                    }
                }
                publishExceptionMetric(request.getAction(), e, HandlerErrorCode.InvalidRequest);
                this.callbackAdapter.reportProgress(request.getBearerToken(), HandlerErrorCode.InvalidRequest,
                    OperationStatus.FAILED, OperationStatus.IN_PROGRESS, null, validationMessageBuilder.toString());
                return ProgressEvent.defaultFailureHandler(new TerminalException(validationMessageBuilder.toString(), e),
                    HandlerErrorCode.InvalidRequest);
            }
        }

        // TODO: implement decryption of request and returned callback context
        // using KMS Key accessible by the Lambda execution Role

        // TODO: implement the handler invocation inside a time check which will abort
        // and automatically
        // reschedule a callback if the handler does not respond within the 15 minute
        // invocation window

        // TODO: ensure that any credential expiry time is also considered in the time
        // check to
        // automatically fail a request if the handler will not be able to complete
        // within that period,
        // such as before a FAS token expires

        // last mile proxy creation with passed-in credentials
        AmazonWebServicesClientProxy awsClientProxy = new AmazonWebServicesClientProxy(requestContext == null, this.loggerProxy,
                                                                                       request.getRequestData()
                                                                                           .getCallerCredentials(),
                                                                                       () -> (long) context
                                                                                           .getRemainingTimeInMillis());

        boolean computeLocally = true;
        ProgressEvent<ResourceT, CallbackT> handlerResponse = null;

        while (computeLocally) {
            // rebuild callback context on each invocation cycle
            requestContext = request.getRequestContext();
            CallbackT callbackContext = (requestContext != null) ? requestContext.getCallbackContext() : null;

            handlerResponse = wrapInvocationAndHandleErrors(awsClientProxy, resourceHandlerRequest, request, callbackContext);

            // report the progress status back to configured endpoint on
            // mutating/potentially asynchronous actions

            if (isMutatingAction) {
                this.callbackAdapter.reportProgress(request.getBearerToken(), handlerResponse.getErrorCode(),
                    handlerResponse.getStatus(), OperationStatus.IN_PROGRESS, handlerResponse.getResourceModel(),
                    handlerResponse.getMessage());
            } else if (handlerResponse.getStatus() == OperationStatus.IN_PROGRESS) {
                throw new TerminalException("READ and LIST handlers must return synchronously.");
            }
            // When the handler responses IN_PROGRESS with a callback delay, we trigger a
            // callback to re-invoke
            // the handler for the Resource type to implement stabilization checks and
            // long-poll creation checks
            computeLocally = scheduleReinvocation(request, handlerResponse, context);
        }

        return handlerResponse;
    }

    private void
        logUnhandledError(final String errorDescription, final HandlerRequest<ResourceT, CallbackT> request, final Throwable e) {
        log(String.format("%s in a %s action on a %s: %s%n%s", errorDescription, request.getAction(), request.getResourceType(),
            e.toString(), ExceptionUtils.getStackTrace(e)));
    }

    /**
     * Invokes the handler implementation for the request, and wraps with try-catch
     * to consistently handle certain classes of errors and correctly map those to
     * the appropriate HandlerErrorCode Also wraps the invocation in last-mile
     * timing metrics
     */
    private ProgressEvent<ResourceT, CallbackT>
        wrapInvocationAndHandleErrors(final AmazonWebServicesClientProxy awsClientProxy,
                                      final ResourceHandlerRequest<ResourceT> resourceHandlerRequest,
                                      final HandlerRequest<ResourceT, CallbackT> request,
                                      final CallbackT callbackContext) {

        Date startTime = Date.from(Instant.now());
        try {
            ProgressEvent<ResourceT, CallbackT> handlerResponse = invokeHandler(awsClientProxy, resourceHandlerRequest,
                request.getAction(), callbackContext);
            if (handlerResponse != null) {
                this.log(String.format("Handler returned %s", handlerResponse.getStatus()));
            } else {
                this.log("Handler returned null");
                throw new TerminalException("Handler failed to provide a response.");
            }

            return handlerResponse;

        } catch (final ResourceAlreadyExistsException e) {
            publishExceptionMetric(request.getAction(), e, HandlerErrorCode.AlreadyExists);
            logUnhandledError("An existing resource was found", request, e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.AlreadyExists);
        } catch (final ResourceNotFoundException e) {
            publishExceptionMetric(request.getAction(), e, HandlerErrorCode.NotFound);
            logUnhandledError("A requested resource was not found", request, e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
        } catch (final AmazonServiceException e) {
            publishExceptionMetric(request.getAction(), e, HandlerErrorCode.GeneralServiceException);
            logUnhandledError("A downstream service error occurred", request, e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.GeneralServiceException);
        } catch (final Throwable e) {
            publishExceptionMetric(request.getAction(), e, HandlerErrorCode.InternalFailure);
            logUnhandledError("An unknown error occurred ", request, e);
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InternalFailure);
        } finally {
            Date endTime = Date.from(Instant.now());
            metricsPublisherProxy.publishDurationMetric(Instant.now(), request.getAction(),
                (endTime.getTime() - startTime.getTime()));
        }

    }

    private Response<ResourceT> createProgressResponse(final ProgressEvent<ResourceT, CallbackT> progressEvent,
                                                       final String bearerToken) {

        Response<ResourceT> response = new Response<>();
        response.setMessage(progressEvent.getMessage());
        response.setOperationStatus(progressEvent.getStatus());
        response.setResourceModel(progressEvent.getResourceModel());
        response.setErrorCode(progressEvent.getErrorCode());
        response.setBearerToken(bearerToken);
        response.setResourceModels(progressEvent.getResourceModels());
        response.setNextToken(progressEvent.getNextToken());

        return response;
    }

    private void writeResponse(final OutputStream outputStream, final Response<ResourceT> response) throws IOException {

        String output = this.serializer.serialize(response);
        outputStream.write(output.getBytes(Charset.forName("UTF-8")));
        outputStream.close();
    }

    private void validateModel(final JSONObject modelObject) throws ValidationException, IOException {
        JSONObject resourceSchemaJSONObject = provideResourceSchemaJSONObject();
        if (resourceSchemaJSONObject == null) {
            throw new ValidationException("Unable to validate incoming model as no schema was provided.", null, null);
        }

        TypeReference<ResourceT> modelTypeReference = getModelTypeReference();

        // deserialize incoming payload to modelled request
        ResourceT deserializedModel;
        try {
            deserializedModel = this.serializer.deserializeStrict(modelObject.toString(), modelTypeReference);
        } catch (UnrecognizedPropertyException e) {
            throw new ValidationException(String.format("#: extraneous key [%s] is not permitted", e.getPropertyName()),
                                          "additionalProperties", "#");

        }

        JSONObject serializedModel = new JSONObject(this.serializer.serialize(deserializedModel));
        this.validator.validateObject(serializedModel, resourceSchemaJSONObject);
    }

    /**
     * Managed scheduling of handler re-invocations.
     *
     * @param request the original request to the function
     * @param handlerResponse the previous response from handler
     * @param context LambdaContext granting runtime metadata
     * @return boolean indicating whether to continue invoking locally, or exit for
     *         async reinvoke
     */
    private boolean scheduleReinvocation(final HandlerRequest<ResourceT, CallbackT> request,
                                         final ProgressEvent<ResourceT, CallbackT> handlerResponse,
                                         final Context context) {

        if (handlerResponse.getStatus() != OperationStatus.IN_PROGRESS) {
            // no reinvoke required
            return false;
        }

        RequestContext<CallbackT> reinvocationContext = new RequestContext<>();
        RequestContext<CallbackT> requestContext = request.getRequestContext();

        int counter = 1;
        if (requestContext != null) {
            counter += requestContext.getInvocation();
        }
        reinvocationContext.setInvocation(counter);

        reinvocationContext.setCallbackContext(handlerResponse.getCallbackContext());
        request.setRequestContext(reinvocationContext);

        // when a handler requests a sub-minute callback delay, and if the lambda
        // invocation
        // has enough runtime (with 20% buffer), we can reschedule from a thread wait
        // otherwise we re-invoke through CloudWatchEvents which have a granularity of
        // minutes
        if ((handlerResponse.getCallbackDelaySeconds() < 60)
            && (context.getRemainingTimeInMillis() / 1000d) > handlerResponse.getCallbackDelaySeconds() * 1.2) {
            log(String.format("Scheduling re-invoke locally after %s seconds, with Context {%s}",
                handlerResponse.getCallbackDelaySeconds(), reinvocationContext.toString()));
            sleepUninterruptibly(handlerResponse.getCallbackDelaySeconds(), TimeUnit.SECONDS);
            return true;
        }

        log(String.format("Scheduling re-invoke with Context {%s}", reinvocationContext.toString()));
        try {
            int callbackDelayMinutes = handlerResponse.getCallbackDelaySeconds() / 60;
            this.scheduler.rescheduleAfterMinutes(context.getInvokedFunctionArn(), callbackDelayMinutes, request);
        } catch (final Throwable e) {
            this.log(String.format("Failed to schedule re-invoke, caused by %s", e.toString()));
            handlerResponse.setMessage(e.getMessage());
            handlerResponse.setStatus(OperationStatus.FAILED);
            handlerResponse.setErrorCode(HandlerErrorCode.InternalFailure);
        }

        return false;
    }

    /**
     * Transforms the incoming request to the subset of typed models which the
     * handler implementor needs
     *
     * @param request The request as passed from the caller (e.g; CloudFormation)
     *            which contains additional context to inform the LambdaWrapper
     *            itself, and is not needed by the handler implementations
     * @return A converted ResourceHandlerRequest model
     */
    protected abstract ResourceHandlerRequest<ResourceT> transform(HandlerRequest<ResourceT, CallbackT> request)
        throws IOException;

    /**
     * Handler implementation should implement this method to provide the schema for
     * validation
     *
     * @return An JSONObject of the resource schema for the provider
     */
    protected abstract JSONObject provideResourceSchemaJSONObject();

    /**
     * Implemented by the handler package as the key entry point.
     */
    public abstract ProgressEvent<ResourceT, CallbackT> invokeHandler(AmazonWebServicesClientProxy proxy,
                                                                      ResourceHandlerRequest<ResourceT> request,
                                                                      Action action,
                                                                      CallbackT callbackContext)
        throws Exception;

    /**
     * null-safe exception metrics delivery
     */
    private void publishExceptionMetric(final Action action, final Throwable ex, final HandlerErrorCode handlerErrorCode) {
        if (this.metricsPublisherProxy != null) {
            this.metricsPublisherProxy.publishExceptionMetric(Instant.now(), action, ex, handlerErrorCode);
        } else {
            // Lambda logger is the only fallback if metrics publisher proxy is not
            // initialized.
            lambdaLogger.log(ex.toString());
        }
    }

    /**
     * null-safe logger redirect
     *
     * @param message A string containing the event to log.
     */
    private void log(final String message) {
        if (this.loggerProxy != null) {
            this.loggerProxy.log(String.format("%s%n", message));
        } else {
            // Lambda logger is the only fallback if metrics publisher proxy is not
            // initialized.
            lambdaLogger.log(message);
        }
    }

    protected abstract TypeReference<HandlerRequest<ResourceT, CallbackT>> getTypeReference();

    protected abstract TypeReference<ResourceT> getModelTypeReference();

    protected void scrubFiles() throws IOException {
        try {
            FileUtils.cleanDirectory(FileUtils.getTempDirectory());
        } catch (IOException e) {
            log(e.getMessage());
            publishExceptionMetric(null, new FileScrubberException(e), HandlerErrorCode.InternalFailure);
        }
    }
}
