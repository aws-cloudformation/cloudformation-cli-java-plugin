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
package software.amazon.cloudformation.proxy;

import java.util.ArrayList;
import java.util.List;

import software.amazon.cloudformation.loggers.LogPublisher;

public class LoggerProxy implements Logger {

    private final List<LogPublisher> logPublishers = new ArrayList<>();

    public void addLogPublisher(final LogPublisher logPublisher) {
        logPublishers.add(logPublisher);
    }

    @Override
    public void log(final String message) {
        logPublishers.stream().forEach(logPublisher -> logPublisher.publishLogEvent(message));
    }
}
