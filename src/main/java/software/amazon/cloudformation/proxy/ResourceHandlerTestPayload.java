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
package software.amazon.cloudformation.proxy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.Action;

/**
 * This POJO is for the test entrypoint that bypasses the wrapper for direct
 * testing.
 *
 * @param <ModelT> Type of resource model being provisioned
 * @param <CallbackT> Type of callback data to be passed on re-invocation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ResourceHandlerTestPayload<ModelT, CallbackT, ConfigurationT> {
    private Credentials credentials;
    private Action action;
    private ResourceHandlerRequest<ModelT> request;
    private CallbackT callbackContext;
    private ConfigurationT typeConfiguration;
}
