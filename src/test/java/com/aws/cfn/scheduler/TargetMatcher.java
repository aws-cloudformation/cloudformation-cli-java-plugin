package com.amazonaws.cloudformation.scheduler;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;

@Data
@AllArgsConstructor
public class TargetMatcher implements ArgumentMatcher<Target> {

    private final String arn;
    private final String id;
    private final Object input;

    @Override
    public boolean matches(final Target argument) {
        return
            argument.arn().equals(arn) &&
            argument.id().startsWith(id) &&
            argument.input().equals(input);
    }
}
