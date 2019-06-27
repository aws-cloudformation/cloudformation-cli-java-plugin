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

public abstract class LogPublisher {

    /**
     * log priority determines the logging order when multiple logger exist. Default
     * to 100. Smaller number is of higher priority, e.g. priority(0) > priority(10)
     */
    protected int priority = 100;

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    /**
     * Method for setting up prerequisites for logging.
     */
    public void initialize() {
    }

    /**
     * Override this method to realize log delivery to expected destination. Boolean
     * indicates delivery log success status.
     *
     * @param message
     * @return
     */
    public abstract boolean publishLogEvent(String message);

    /**
     * Redact or scrub loggers in someway to help prevent leaking of certain
     * information.
     */
    protected String filterMessage(final String message) {
        // Default filtering mechanism to be determined.
        // Subclass could override this method for specific purpose.
        return message;
    }
}
