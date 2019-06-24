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
package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.Action;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This interface describes the request object for the provisioning request
 */
@Data
@NoArgsConstructor
public class HandlerRequest<ResourceT, CallbackT> {
    private Action action;
    private String awsAccountId;
    private String bearerToken;
    private String nextToken;
    private String region;
    private String responseEndpoint;
    private String resourceType;
    private String resourceTypeVersion;
    private RequestData<ResourceT> requestData;
    private String stackId;
    private RequestContext<CallbackT> requestContext;
}
