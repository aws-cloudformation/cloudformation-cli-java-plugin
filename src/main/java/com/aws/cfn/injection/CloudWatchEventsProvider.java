package com.aws.cfn.injection;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;

public class CloudWatchEventsProvider extends AmazonWebServicesProvider {

    public CloudWatchEventsProvider(final CredentialsProvider credentialsProvider) {
        super(credentialsProvider);
    }

    public CloudWatchEventsClient get() {
        return CloudWatchEventsClient.builder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                //Default Retry Condition of Retry Policy retries on Throttling and ClockSkew Exceptions
                .retryPolicy(RetryPolicy.builder()
                    .numRetries(16)
                    .build())
                .build())
            .credentialsProvider(this.getCredentialsProvider())
            .build();
    }
}
