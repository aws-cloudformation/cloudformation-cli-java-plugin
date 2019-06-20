package com.amazonaws.cloudformation.injection;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import java.util.List;
import java.util.stream.Collectors;

public class CloudWatchProvider extends AmazonWebServicesProvider {

    public CloudWatchProvider(final CredentialsProvider... credentialsProviders) {
        super(credentialsProviders);
    }

    public List<CloudWatchClient> getClients() {
       return this.getCredentialsProviders().stream().map(awsCredentialsProvider -> CloudWatchClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        //Default Retry Condition of Retry Policy retries on Throttling and ClockSkew Exceptions
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(16)
                                .build())
                        .build())
                .build()).collect(Collectors.toList());
    }
}
