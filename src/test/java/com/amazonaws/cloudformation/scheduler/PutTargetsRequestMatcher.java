/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the &quot;License&quot;).
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the &quot;license&quot; file accompanying this file. This file is distributed
* on an &quot;AS IS&quot; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.scheduler;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.mockito.ArgumentMatcher;

import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;

@Data
@AllArgsConstructor
public class PutTargetsRequestMatcher implements ArgumentMatcher<PutTargetsRequest> {

    private final String rule;
    private final ArgumentMatcher<List<Target>> targetArgumentMatcher;

    @Override
    public boolean matches(final PutTargetsRequest argument) {
        return argument.rule().startsWith(rule) && targetArgumentMatcher.matches(argument.targets());
    }
}
