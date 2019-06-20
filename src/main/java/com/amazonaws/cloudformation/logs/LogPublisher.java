package com.amazonaws.cloudformation.logs;

public interface LogPublisher {
    /**
     * Redact or scrub logs in someway to help prevent leaking of certain information.
     */
    void filterLogMessage();

    /**
     * Method for setting up prerequisites for logging.
     */
    void initialize();

    /**
     * Override this method to realize log delivery to expected destination.
     * @param message
     */
    void publishLogEvent(final String message);

}
