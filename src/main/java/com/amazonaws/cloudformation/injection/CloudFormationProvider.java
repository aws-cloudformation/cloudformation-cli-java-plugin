/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the &quot;License&quot;).
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the &quot;license&quot; file accompanying this file. This file is distributed
* on an &quot;AS IS&quot; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
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
