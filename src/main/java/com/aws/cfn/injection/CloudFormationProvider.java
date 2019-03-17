package com.aws.cfn.injection;

import com.google.inject.Provider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;

import java.net.URI;

public class CloudFormationProvider implements Provider<CloudFormationAsyncClient> {

    @Override
    public CloudFormationAsyncClient get() {
        final URI endpointConfiguration = URI.create("https://stackbuilder-2.amazonaws.com");

        return CloudFormationAsyncClient.builder()
            .endpointOverride(endpointConfiguration)
            .region(Region.EU_WEST_1)
            .build();
    }
}
