package com.aws.cfn;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.PutRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.PutTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.RuleState;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.StringUtils;
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
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static com.aws.cfn.cron.CronHelper.generateOneTimeCronExpression;

public abstract class LambdaWrapper<T> implements RequestStreamHandler, RequestHandler<Request<T>, Response> {

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
                new TypeToken<HandlerRequestImpl<T>>(){}.getType());

        if (request == null || request.getRequestContext() == null) {
            writeResponse(
                outputStream,
                createErrorResponse(String.format("Invalid request object received (%s)", input))
            );
            return;
        }

        final RequestContext requestContext = request.getRequestContext();

        // If this invocation was triggered by a 're-invoke' CloudWatch Event, clean it up
        this.cleanupCloudWatchEvents(
            requestContext.getCloudWatchEventsRuleName(),
            requestContext.getCloudWatchEventsTargetId());

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
            final RequestContext callbackContext = new RequestContext(
                requestContext.getInvocation() + 1,
                handlerResponse.getCallbackContext(),
                requestContext.getResourceType());

            rescheduleAfterMinutes(
                context.getInvokedFunctionArn(),
                handlerResponse.getCallbackDelayMinutes(),
                callbackContext);
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
     * TODO: this actually needs testing and such, but you get the idea
     */
    private void rescheduleAfterMinutes(final String functionArn,
                                        final int minutesFromNow,
                                        final RequestContext callbackContext) {

        final String cronRule = generateOneTimeCronExpression(minutesFromNow);

        final AmazonCloudWatchEvents cloudWatch = AmazonCloudWatchEventsClientBuilder.defaultClient();

        final UUID rescheduleId = UUID.randomUUID();
        final String ruleName = String.format("reinvoke-handler-%s", rescheduleId);
        final String targetId = String.format("reinvoke-target-%s", rescheduleId);

        // record the CloudWatchEvents objects for cleanup on the callback
        callbackContext.setCloudWatchEventsRuleName(ruleName);
        callbackContext.setCloudWatchEventsTargetId(targetId);

        final String jsonContext = new Gson().toJson(callbackContext);

        this.logger.log(String.format("Scheduling re-invoke at %s (%s)\n", cronRule, rescheduleId));
        this.logger.log(String.format("Context: (%s)\n", jsonContext));

        final PutRuleRequest putRuleRequest = new PutRuleRequest()
            .withName(ruleName)
            .withScheduleExpression(cronRule)
            .withState(RuleState.ENABLED);
        cloudWatch.putRule(putRuleRequest);

        final Target target = new Target()
            .withArn(functionArn)
            .withId(targetId)
            .withInput(jsonContext);
        final PutTargetsRequest putTargetsRequest = new PutTargetsRequest()
            .withTargets(target)
            .withRule(putRuleRequest.getName());
        cloudWatch.putTargets(putTargetsRequest);
    }

    private void cleanupCloudWatchEvents(final String cloudWatchEventsRuleName,
                                         final String cloudWatchEventsTargetId) {

        final AmazonCloudWatchEvents cloudWatch = AmazonCloudWatchEventsClientBuilder.defaultClient();

        try {
            if (!StringUtils.isNullOrEmpty(cloudWatchEventsRuleName)) {
                final DeleteRuleRequest deleteRuleRequest = new DeleteRuleRequest()
                    .withName(cloudWatchEventsRuleName);
                cloudWatch.deleteRule(deleteRuleRequest);
            }
        } catch (final Exception e) {
            this.logger.log(String.format("Error cleaning CloudWatchEvents (ruleName=%s): %s",
                cloudWatchEventsRuleName,
                e.getMessage()));
        }

        try {
            if (!StringUtils.isNullOrEmpty(cloudWatchEventsTargetId)) {
                final RemoveTargetsRequest removeTargetsRequest = new RemoveTargetsRequest()
                    .withIds(cloudWatchEventsTargetId);
                cloudWatch.removeTargets(removeTargetsRequest);
            }
        } catch (final Exception e) {
            this.logger.log(String.format("Error cleaning CloudWatchEvents Target (targetId=%s): %s",
                cloudWatchEventsTargetId,
                e.getMessage()));
        }
    }

    public abstract ProgressEvent invokeHandler(final HandlerRequest<T> request,
                                                final Action action,
                                                final RequestContext context);
}
