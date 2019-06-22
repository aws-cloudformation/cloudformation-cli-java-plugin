package com.amazonaws.cloudformation.loggers;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class LambdaLogPublisherImpl extends LogPublisher {

    private final LambdaLogger logger;

    public LambdaLogPublisherImpl(final LambdaLogger logger) {
        // Make LambdaLogger have higher than default priority.
        this.logger = logger;
    }

    @Override
    public boolean publishLogEvent(String message) {
        this.logger.log(
            filterMessage(message)
        );
        return true;
    }
}
