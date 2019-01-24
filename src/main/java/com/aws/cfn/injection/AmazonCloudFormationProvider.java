package com.aws.cfn.injection;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.google.inject.Provider;

public class AmazonCloudFormationProvider implements Provider<AmazonCloudFormation> {

    @Override
    public AmazonCloudFormation get() {
         final EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
            "https://stackbuilder.us-west-2.amazonaws.com",
        "us-west-2");

        return AmazonCloudFormationClientBuilder.standard()
            .withEndpointConfiguration(endpointConfiguration)
            .build();
    }
}
