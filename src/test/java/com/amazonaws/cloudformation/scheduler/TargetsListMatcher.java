package com.amazonaws.cloudformation.scheduler;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.mockito.ArgumentMatcher;

import software.amazon.awssdk.services.cloudwatchevents.model.Target;

@Data
@AllArgsConstructor
public class TargetsListMatcher implements ArgumentMatcher<List<Target>> {

    private final List<TargetMatcher> targetMatchers;

    @Override
    public boolean matches(final List<Target> argument) {
        for (int i = 0; i < argument.size(); i++) {
            if (!targetMatchers.get(i).matches(argument.get(i))) {
                return false;
            }
        }
        return true;
    }
}
