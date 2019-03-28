package com.aws.cfn.scheduler;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;

import java.util.List;

@Data
@AllArgsConstructor
public class PutTargetsRequestMatcher implements ArgumentMatcher<PutTargetsRequest> {

    private final String rule;
    private final ArgumentMatcher<List<Target>> targetArgumentMatcher;

    @Override
    public boolean matches(final PutTargetsRequest argument) {
        return
            argument.rule().startsWith(rule) &&
            targetArgumentMatcher.matches(argument.targets());
    }
}
