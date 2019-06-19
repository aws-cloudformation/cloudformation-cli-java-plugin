package com.amazonaws.cloudformation.logs;

public interface LogPublisher {
    void publishLogEvent(final String messageToLog);
    void initializeLoggingConditions();
}
