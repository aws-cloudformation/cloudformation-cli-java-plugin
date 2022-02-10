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
import software.amazon.cloudformation.proxy.Credentials;

/**
 * This POJO is for the test entrypoint that bypasses the wrapper for direct
 * testing.
 *
 * @param <ConfigurationT> Type of hook type configuration model
 * @param <CallbackT> Type of callback data to be passed on re-invocation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class HookHandlerTestPayload<ConfigurationT, CallbackT> {
    private Credentials credentials;
    private HookInvocationPoint actionInvocationPoint;
    private HookHandlerRequest request;
    private CallbackT callbackContext;
    private ConfigurationT typeConfiguration;
}
