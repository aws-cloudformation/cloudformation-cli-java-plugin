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
package software.amazon.cloudformation.proxy.service;

import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

public class NotFoundException extends AwsServiceException {
    private static final long serialVersionUID = 1L;
    private final SdkHttpResponse response;

    public NotFoundException(AwsServiceException.Builder builder) {
        super(builder);
        response = Mockito.mock(SdkHttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(404);
    }

    @Override
    public AwsErrorDetails awsErrorDetails() {
        return AwsErrorDetails.builder().errorCode("NotFound").errorMessage("Repo not existing").sdkHttpResponse(response)
            .build();
    }
}
