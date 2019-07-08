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
package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.loggers.LogPublisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Proxies logging requests to the passed in loggers. NOTE: Logging order is
 * determined by loggers priorities, which is not guaranteed if having same
 * priorities.
 */
public class LoggerProxy implements Logger {

    private final List<LogPublisher> logPublishers = new ArrayList<>();

    public void addLogPublisher(final LogPublisher logPublisher) {
        logPublishers.add(logPublisher);
        logPublishers.sort(Comparator.comparingInt(LogPublisher::getPriority));
    }

    @Override
    public void log(final String message) {
        logPublishers.stream().forEach(logPublisher -> {
            logPublisher.publishLogEvent(logPublisher.filterMessage(message));
        });
    }
}
