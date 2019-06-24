package com.amazonaws.cloudformation.injection;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

public class CloudWatchProvider extends AmazonWebServicesProvider {

    public CloudWatchProvider(final CredentialsProvider credentialsProvider) {
        super(credentialsProvider);
    }

    public CloudWatchClient get() {
        return CloudWatchClient.builder().credentialsProvider(this.getCredentialsProvider())
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                // Default Retry Condition of Retry Policy retries on Throttling and ClockSkew
                // Exceptions
                .retryPolicy(RetryPolicy.builder().numRetries(16).build()).build())
            .build();
    }
}
