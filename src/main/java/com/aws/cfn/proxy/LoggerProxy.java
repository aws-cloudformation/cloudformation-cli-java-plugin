package com.aws.cfn.proxy;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * Proxies logging requests to the default LambdaLogger (CloudWatch Logs)
 */
public final class LoggerProxy implements Logger {

    private final LambdaLogger lambdaLogger;

    public LoggerProxy(final LambdaLogger lambdaLogger) {
        this.lambdaLogger = lambdaLogger;
    }

    @Override
    public void log(final String message) {
        lambdaLogger.log(message);
    }
}
