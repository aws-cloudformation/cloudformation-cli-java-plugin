package com.amazonaws.cloudformation.injection;

import java.net.URI;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public class CloudFormationProvider extends AmazonWebServicesProvider {

    private URI callbackEndpoint;

    public CloudFormationProvider(final CredentialsProvider credentialsProvider) {
        super(credentialsProvider);
    }

    public void setCallbackEndpoint(final URI callbackEndpoint) {
        this.callbackEndpoint = callbackEndpoint;
    }

    public CloudFormationClient get() {
        return CloudFormationClient.builder().credentialsProvider(this.getCredentialsProvider())
            .endpointOverride(this.callbackEndpoint).overrideConfiguration(ClientOverrideConfiguration.builder()
                // Default Retry Condition of Retry Policy retries on Throttling and ClockSkew
                // Exceptions
                .retryPolicy(RetryPolicy.builder().numRetries(16).build()).build())
            .build();
    }
}
