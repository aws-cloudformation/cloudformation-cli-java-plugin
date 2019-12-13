/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.injection;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;

public abstract class AmazonWebServicesProvider {

    protected static final ClientOverrideConfiguration CONFIGURATION = ClientOverrideConfiguration.builder()
        // Default Retry Condition of Retry Policy retries on Throttling and ClockSkew
        // Exceptions
        .retryPolicy(RetryPolicy.builder().numRetries(16).build()).build();

    protected final CredentialsProvider credentialsProvider;
    protected final SdkHttpClient httpClient;

    protected AmazonWebServicesProvider(final CredentialsProvider credentialsProvider,
                                        final SdkHttpClient httpClient) {
        this.credentialsProvider = credentialsProvider;
        this.httpClient = httpClient;
    }

    protected AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(this.credentialsProvider.get());
    }

    protected <BuilderT extends AwsClientBuilder<BuilderT, ClientT> & AwsSyncClientBuilder<BuilderT, ClientT>,
        ClientT> BuilderT defaultClient(final BuilderT builder) {
        return builder.credentialsProvider(this.getCredentialsProvider()).overrideConfiguration(CONFIGURATION)
            .httpClient(httpClient);
    }
}
