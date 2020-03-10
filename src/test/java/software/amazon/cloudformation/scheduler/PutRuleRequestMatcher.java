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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RuleState;

@Data
@AllArgsConstructor
public class PutRuleRequestMatcher implements ArgumentMatcher<PutRuleRequest> {

    private String name;
    private String scheduleExpression;
    private RuleState state;

    @Override
    public boolean matches(final PutRuleRequest argument) {
        return argument.description() == null && argument.eventPattern() == null && argument.name().startsWith(name)
            && argument.roleArn() == null && argument.scheduleExpression().equals(scheduleExpression)
            && argument.state().equals(state);
    }
}
