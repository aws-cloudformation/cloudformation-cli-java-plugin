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
package software.amazon.cloudformation.proxy.hook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.HookInvocationPoint;

/**
 * This interface describes the request object for the hook invocation request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class HookInvocationRequest<ConfigurationT, CallbackT> {
    private String clientRequestToken;
    private String awsAccountId;
    private String stackId;
    private String changeSetId;
    private String hookTypeName;
    private String hookTypeVersion;
    private ConfigurationT hookModel;
    private HookInvocationPoint actionInvocationPoint;
    private HookRequestData requestData;
    private HookRequestContext<CallbackT> requestContext;
}
