package com.aws.cfn.injection;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public class CloudFormationProvider extends AmazonWebServicesProvider {

    public CloudFormationProvider(final CredentialsProvider credentialsProvider) {
        super(credentialsProvider);
    }

    public CloudFormationClient get() {
        return CloudFormationClient.builder()
                .credentialsProvider(this.getCredentialsProvider())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        //Default Retry Condition of Retry Policy retries on Throttling and ClockSkew Exceptions
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(16)
                                .build())
                        .build())
                .build();
    }
}
