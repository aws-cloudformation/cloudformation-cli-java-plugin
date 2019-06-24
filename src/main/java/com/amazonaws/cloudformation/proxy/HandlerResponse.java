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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This interface describes the response object for the provisioning request
 *
 * @param <ResourceT> Type of resource model being provisioned
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HandlerResponse<ResourceT> {
    private String bearerToken;
    private String errorCode;
    private String message;
    private String nextToken;
    private OperationStatus operationStatus;
    private ResourceT resourceModel;
    private StabilizationData stabilizationData;
}
