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

import java.net.URI;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public class CloudFormationProvider extends AmazonWebServicesProvider {

    private URI callbackEndpoint;

    public CloudFormationProvider(final CredentialsProvider credentialsProvider,
                                  final SdkHttpClient httpClient) {
        super(credentialsProvider, httpClient);
    }

    public void setCallbackEndpoint(final URI callbackEndpoint) {
        this.callbackEndpoint = callbackEndpoint;
    }

    public CloudFormationClient get() {
        return CloudFormationClient.builder().credentialsProvider(this.getCredentialsProvider()).httpClient(httpClient)
            .endpointOverride(this.callbackEndpoint).build();
    }
}
