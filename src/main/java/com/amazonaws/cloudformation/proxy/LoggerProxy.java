package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.loggers.LogPublisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Proxies logging requests to the passed in loggers.
 * NOTE: Logging order is determined by loggers priorities, which is not guaranteed if having same priorities.
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
            logPublisher.publishLogEvent(message);
        });
    }
}
