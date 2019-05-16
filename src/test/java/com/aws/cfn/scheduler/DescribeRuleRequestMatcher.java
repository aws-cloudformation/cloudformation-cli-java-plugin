package com.aws.cfn.scheduler;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;

@Data
@AllArgsConstructor
public class DescribeRuleRequestMatcher implements ArgumentMatcher<DescribeRuleRequest> {

    private String name;

    @Override
    public boolean matches(final DescribeRuleRequest argument) {
        return argument.name().startsWith(name);
    }
}
