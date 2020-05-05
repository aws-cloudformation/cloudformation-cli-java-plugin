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
package software.amazon.cloudformation.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RuleState;
import software.amazon.cloudformation.TestContext;
import software.amazon.cloudformation.TestModel;
import software.amazon.cloudformation.injection.CloudWatchEventsProvider;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.RequestContext;
import software.amazon.cloudformation.resource.Serializer;

@ExtendWith(MockitoExtension.class)
public class CloudWatchSchedulerTest {

    @Mock
    private Logger loggerProxy;

    @Mock
    private RequestContext<TestContext> requestContext;
    private final Serializer serializer = new Serializer();

    private static final String FUNCTION_ARN = "arn:aws:lambda:region:account-id:function:function-name";

    private CronHelper getCronHelper() {
        return mock(CronHelper.class);
    }

    private CloudWatchEventsClient getCloudWatchEvents() {
        return mock(CloudWatchEventsClient.class);
    }

    @Test
    public void test_cleanupCloudWatchEvents_NullRuleName() {
        final CloudWatchEventsProvider provider = mock(CloudWatchEventsProvider.class);
        final CloudWatchEventsClient client = getCloudWatchEvents();
        when(provider.get()).thenReturn(client);
        final CronHelper cronHelper = getCronHelper();
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper, serializer);
        scheduler.refreshClient();

        scheduler.cleanupCloudWatchEvents(null, "targetid");

        verify(client, times(0)).deleteRule(any(DeleteRuleRequest.class));

        verify(client, times(1)).removeTargets(any(RemoveTargetsRequest.class));
    }

    @Test
    public void test_cleanupCloudWatchEvents_NullTargetId() {
        final CloudWatchEventsProvider provider = mock(CloudWatchEventsProvider.class);
        final CloudWatchEventsClient client = getCloudWatchEvents();
        when(provider.get()).thenReturn(client);
        final CronHelper cronHelper = getCronHelper();
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper, serializer);
        scheduler.refreshClient();

        scheduler.cleanupCloudWatchEvents("rulename", null);

        verify(client, times(1)).deleteRule(any(DeleteRuleRequest.class));

        verify(client, times(0)).removeTargets(any(RemoveTargetsRequest.class));
    }

    @Test
    public void test_cleanupCloudWatchEvents() {
        final CloudWatchEventsProvider provider = mock(CloudWatchEventsProvider.class);
        final CloudWatchEventsClient client = getCloudWatchEvents();
        when(provider.get()).thenReturn(client);
        final CronHelper cronHelper = getCronHelper();
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper, serializer);
        scheduler.refreshClient();

        scheduler.cleanupCloudWatchEvents("rulename", "targetid");

        verify(client, times(1)).deleteRule(any(DeleteRuleRequest.class));

        verify(client, times(1)).removeTargets(any(RemoveTargetsRequest.class));
    }

    @Test
    public void test_cleanupCloudWatchEventsWithErrorRemovingTarget() {
        final CloudWatchEventsProvider provider = mock(CloudWatchEventsProvider.class);
        final CloudWatchEventsClient client = getCloudWatchEvents();
        when(provider.get()).thenReturn(client);
        final CronHelper cronHelper = getCronHelper();
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper, serializer);
        when(client.deleteRule(ArgumentCaptor.forClass(DeleteRuleRequest.class).capture()))
            .thenThrow(new RuntimeException("AccessDenied"));

        scheduler.refreshClient();

        scheduler.cleanupCloudWatchEvents("rulename", "targetid");

        verify(client, times(1)).deleteRule(any(DeleteRuleRequest.class));

        verify(client, times(1)).removeTargets(any(RemoveTargetsRequest.class));
    }

    @Test
    public void test_cleanupCloudWatchEventsWithErrorDeletingRule() {
        final CloudWatchEventsProvider provider = mock(CloudWatchEventsProvider.class);
        final CloudWatchEventsClient client = getCloudWatchEvents();
        when(provider.get()).thenReturn(client);
        final CronHelper cronHelper = getCronHelper();
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, null, cronHelper, serializer);
        when(client.removeTargets(ArgumentCaptor.forClass(RemoveTargetsRequest.class).capture()))
            .thenThrow(new RuntimeException("AccessDenied"));

        scheduler.refreshClient();

        scheduler.cleanupCloudWatchEvents("rulename", "targetid");

        verify(client, times(1)).deleteRule(any(DeleteRuleRequest.class));

        verify(client, times(1)).removeTargets(any(RemoveTargetsRequest.class));
    }

    @Test
    public void test_rescheduleAfterMinutes_1MinuteFloor() throws IOException {
        final CloudWatchEventsProvider provider = mock(CloudWatchEventsProvider.class);
        final CloudWatchEventsClient client = getCloudWatchEvents();
        when(provider.get()).thenReturn(client);
        final CronHelper cronHelper = getCronHelper();
        when(cronHelper.generateOneTimeCronExpression(1)).thenReturn("cron(41 14 31 10 ? 2019)");
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper, serializer);
        scheduler.refreshClient();
        final HandlerRequest<TestModel, TestContext> request = new HandlerRequest<>();

        request.setRequestContext(requestContext);

        // minutesFromNow will be set to a floor of '1' for cron generation
        scheduler.rescheduleAfterMinutes(FUNCTION_ARN, 0, request);

        verify(requestContext, times(1)).setCloudWatchEventsRuleName(startsWith("reinvoke-handler-"));
        verify(requestContext, times(1)).setCloudWatchEventsTargetId(startsWith("reinvoke-target-"));

        final List<TargetMatcher> targetMatchers = Collections.singletonList(
            new TargetMatcher(FUNCTION_ARN, "reinvoke-target-", serializer.compress(serializer.serialize(request))));

        verify(client, times(1))
            .putTargets(argThat(new PutTargetsRequestMatcher("reinvoke-handler-", new TargetsListMatcher(targetMatchers))));
        verify(client, times(1))
            .putRule(argThat(new PutRuleRequestMatcher("reinvoke-handler-", "cron(41 14 31 10 ? 2019)", RuleState.ENABLED)));
    }
}
