package com.amazonaws.cloudformation.proxy;

public interface Logger {

    /**
     * Log a message to the default provider on this runtime.
     * @param message   the message to emit to log
     */
    void log(String message);

}
