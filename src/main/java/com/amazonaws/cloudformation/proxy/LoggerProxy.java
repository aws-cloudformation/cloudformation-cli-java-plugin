package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.logs.LogPublisher;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * Proxies logging requests to the default LambdaLogger (CloudWatch Logs)
 */
public final class LoggerProxy implements Logger {

    private final LambdaLogger platformLambdaLogger;
    private final LogPublisher resourceOwnerEventsLogger;

    public LoggerProxy(final LambdaLogger lambdaLogger, final LogPublisher resourceOwnerEventsLogger) {
        this.platformLambdaLogger = lambdaLogger;
        this.resourceOwnerEventsLogger = resourceOwnerEventsLogger;
    }

    @Override
    public void log(final String message) {
        if (platformLambdaLogger != null) {
            platformLambdaLogger.log(message);
        }
        if (resourceOwnerEventsLogger != null) {
            //NOTE: Exceptions might be thrown are handled inside LogPublisher.
            resourceOwnerEventsLogger.publishLogEvent(message);
        }
    }
}
