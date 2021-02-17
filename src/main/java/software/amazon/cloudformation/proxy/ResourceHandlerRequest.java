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

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This interface describes the request object for the provisioning request
 * passed to the implementor. It is transformed from an instance of
 * HandlerRequest by the LambdaWrapper to only items of concern
 *
 * @param <T> Type of resource model being provisioned
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ResourceHandlerRequest<T> {
    private String clientRequestToken;
    private T desiredResourceState;
    private T previousResourceState;
    private Map<String, String> desiredResourceTags;
    private Map<String, String> previousResourceTags;
    private Map<String, String> systemTags;
    private Map<String, String> previousSystemTags;
    private String awsAccountId;
    private String awsPartition;
    private String logicalResourceIdentifier;
    private String nextToken;
    private Boolean snapshotRequested;
    private Boolean rollback;
    private Boolean driftable;
    private String region;
    private String stackId;
}
