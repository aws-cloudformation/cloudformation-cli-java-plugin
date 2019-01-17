package com.aws.cfn.scheduler;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.amazonaws.services.cloudwatchevents.model.DeleteRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.PutRuleRequest;
import com.amazonaws.services.cloudwatchevents.model.PutTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.RemoveTargetsRequest;
import com.amazonaws.services.cloudwatchevents.model.RuleState;
import com.amazonaws.services.cloudwatchevents.model.Target;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringUtils;
import com.aws.cfn.proxy.RequestContext;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONObject;

import java.util.UUID;

import static com.aws.cfn.cron.CronHelper.generateOneTimeCronExpression;

@Data
@NoArgsConstructor
public class CloudWatchScheduler {

    private LambdaLogger logger;

    /**
     * Schedule a re-invocation of the executing handler no less than 1 minute from now
     * @param functionArn       the ARN oft he Lambda function to be invoked
     * @param minutesFromNow    the minimum minutes from now that the re-invocation will occur. CWE provides only
     *                          minute-granularity
     * @param callbackContext   additional context which the handler can provide itself for re-invocation
     */
    public void rescheduleAfterMinutes(final String functionArn,
                                       final int minutesFromNow,
                                       final RequestContext callbackContext) {

        // generate a cron expression; minutes must be a positive integer
        final String cronRule = generateOneTimeCronExpression(Math.max(minutesFromNow, 1));

        final AmazonCloudWatchEvents cloudWatch = AmazonCloudWatchEventsClientBuilder.defaultClient();

        final UUID rescheduleId = UUID.randomUUID();
        final String ruleName = String.format("reinvoke-handler-%s", rescheduleId);
        final String targetId = String.format("reinvoke-target-%s", rescheduleId);

        // record the CloudWatchEvents objects for cleanup on the callback
        callbackContext.setCloudWatchEventsRuleName(ruleName);
        callbackContext.setCloudWatchEventsTargetId(targetId);

        final String jsonContext = new JSONObject(callbackContext).toString();
        //final String jsonContext = new Gson().toJson(callbackContext);

        this.log(String.format("Scheduling re-invoke at %s (%s)\n", cronRule, rescheduleId));
        this.log(String.format("Context: (%s)\n", jsonContext));

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

    /**
     * After a re-invocation, the CWE rule which generated the reinvocation should be scrubbed
     * @param cloudWatchEventsRuleName  the name of the CWE rule which triggered a re-invocation
     * @param cloudWatchEventsTargetId  the target of the CWE rule which triggered a re-invocation
     */
    public void cleanupCloudWatchEvents(final String cloudWatchEventsRuleName,
                                        final String cloudWatchEventsTargetId) {

        final AmazonCloudWatchEvents cloudWatch = AmazonCloudWatchEventsClientBuilder.defaultClient();

        try {
            if (!StringUtils.isNullOrEmpty(cloudWatchEventsRuleName)) {
                final DeleteRuleRequest deleteRuleRequest = new DeleteRuleRequest()
                    .withName(cloudWatchEventsRuleName);
                cloudWatch.deleteRule(deleteRuleRequest);
            }
        } catch (final Exception e) {
            this.log(String.format("Error cleaning CloudWatchEvents (ruleName=%s): %s",
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
            this.log(String.format("Error cleaning CloudWatchEvents Target (targetId=%s): %s",
                cloudWatchEventsTargetId,
                e.getMessage()));
        }
    }

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
