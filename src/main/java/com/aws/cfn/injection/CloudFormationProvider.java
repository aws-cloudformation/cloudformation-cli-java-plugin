package com.aws.cfn.injection;

import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;

public class CloudFormationProvider extends AmazonWebServicesProvider {

    public CloudFormationProvider(final PlatformCredentialsProvider platformCredentialsProvider) {
        super(platformCredentialsProvider);
    }

    public CloudFormationAsyncClient get() {
        return CloudFormationAsyncClient.builder()
            .credentialsProvider(this.getCredentialsProvider())
            .build();
    }
}
