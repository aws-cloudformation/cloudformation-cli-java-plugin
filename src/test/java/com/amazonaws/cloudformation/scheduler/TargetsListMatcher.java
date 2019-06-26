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
