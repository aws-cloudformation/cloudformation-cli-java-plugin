package com.aws.cfn.scheduler;

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
        return
            argument.description() == null &&
            argument.eventPattern() == null &&
            argument.name().startsWith(name) &&
            argument.roleArn() == null &&
            argument.scheduleExpression().equals(scheduleExpression) &&
            argument.state().equals(state);
    }
}
