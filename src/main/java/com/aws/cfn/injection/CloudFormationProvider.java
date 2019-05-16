package com.aws.cfn.injection;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public class CloudFormationProvider extends AmazonWebServicesProvider {

    public CloudFormationProvider(final PlatformCredentialsProvider platformCredentialsProvider) {
        super(platformCredentialsProvider);
    }

    public CloudFormationClient get() {
        return CloudFormationClient.builder()
                .credentialsProvider(this.getCredentialsProvider())
                .build();
    }
}
