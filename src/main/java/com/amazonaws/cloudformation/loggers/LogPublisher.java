package com.amazonaws.cloudformation.loggers;

public abstract class LogPublisher {

    /**
     * log priority determines the logging order when multiple logger exist. Default to 100.
     * Smaller number is of higher priority, e.g. priority(0) > priority(10)
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
    public void initialize() {}

    /**
     * Override this method to realize log delivery to expected destination. Boolean indicates delivery log success status.
     * @param message
     * @return
     */
    public abstract boolean publishLogEvent(final String message);

    /**
     * Redact or scrub loggers in someway to help prevent leaking of certain information.
     */
    protected String filterMessage(final String message) {
        // Default filtering mechanism to be determined.
        // Subclass could override this method for specific purpose.
        return message;
    }
}
