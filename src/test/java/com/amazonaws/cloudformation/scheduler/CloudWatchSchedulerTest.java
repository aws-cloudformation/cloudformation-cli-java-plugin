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
package com.amazonaws.cloudformation.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.cloudformation.TestContext;
import com.amazonaws.cloudformation.TestModel;
import com.amazonaws.cloudformation.injection.CloudWatchEventsProvider;
import com.amazonaws.cloudformation.proxy.HandlerRequest;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.RequestContext;

import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RuleState;

@ExtendWith(MockitoExtension.class)
public class CloudWatchSchedulerTest {

    @Mock
    private Logger loggerProxy;

    @Mock
    private RequestContext<TestContext> requestContext;

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
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper);
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
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper);
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
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper);
        scheduler.refreshClient();

        scheduler.cleanupCloudWatchEvents("rulename", "targetid");

        verify(client, times(1)).deleteRule(any(DeleteRuleRequest.class));

        verify(client, times(1)).removeTargets(any(RemoveTargetsRequest.class));
    }

    @Test
    public void test_rescheduleAfterMinutes_1MinuteFloor() {
        final CloudWatchEventsProvider provider = mock(CloudWatchEventsProvider.class);
        final CloudWatchEventsClient client = getCloudWatchEvents();
        when(provider.get()).thenReturn(client);
        final CronHelper cronHelper = getCronHelper();
        when(cronHelper.generateOneTimeCronExpression(1)).thenReturn("cron(41 14 31 10 ? 2019)");
        final CloudWatchScheduler scheduler = new CloudWatchScheduler(provider, loggerProxy, cronHelper);
        scheduler.refreshClient();
        final HandlerRequest<TestModel, TestContext> request = new HandlerRequest<>();

        request.setRequestContext(requestContext);

        // minutesFromNow will be set to a floor of '1' for cron generation
        scheduler.rescheduleAfterMinutes(FUNCTION_ARN, 0, request);

        verify(requestContext, times(1)).setCloudWatchEventsRuleName(startsWith("reinvoke-handler-"));
        verify(requestContext, times(1)).setCloudWatchEventsTargetId(startsWith("reinvoke-target-"));

        final List<TargetMatcher> targetMatchers = Arrays
            .asList(new TargetMatcher(FUNCTION_ARN, "reinvoke-target-", new JSONObject(request).toString()));
        verify(client, times(1))
            .putTargets(argThat(new PutTargetsRequestMatcher("reinvoke-handler-", new TargetsListMatcher(targetMatchers))));
        verify(client, times(1))
            .putRule(argThat(new PutRuleRequestMatcher("reinvoke-handler-", "cron(41 14 31 10 ? 2019)", RuleState.ENABLED)));
        verify(client, times(1)).describeRule(argThat(new DescribeRuleRequestMatcher("reinvoke-handler-")));
    }
}
