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
package com.amazonaws.cloudformation.loggers;

import java.util.Arrays;
import java.util.List;

public abstract class LogPublisher {
    private List<LogFilter> logFilterList;

    /**
     * Constructor with a sequence of logFilters.
     */
    public LogPublisher(final LogFilter... filters) {
        logFilterList = Arrays.asList(filters);
    }

    public void refreshClient() {
    }

    /**
     * Override to implement the log delivery method to destinations.
     *
     * @param message
     * @return
     */
    protected abstract void publishMessage(String message);

    /**
     * Redact or scrub loggers in someway to help prevent leaking of certain
     * information.
     */
    private String filterMessage(final String message) {
        // Default filtering mechanism to be determined.
        // Subclass could override this method for specific purpose.
        String toReturn = message;
        for (LogFilter filter : logFilterList) {
            toReturn = filter.filterString(toReturn);
        }
        return toReturn;
    }

    /**
     * Entry point of log publisher.
     *
     * @param message
     */
    public final void publishLogEvent(final String message) {
        publishMessage(filterMessage(message));
    }

}
