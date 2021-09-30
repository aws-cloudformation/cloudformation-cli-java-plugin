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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.HookInvocationPoint;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;

/**
 * This interface describes the hook context object for the hook invocation
 * request passed to the implementor.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class HookContext {
    private String awsAccountId;
    private String stackId;
    private String changeSetId;
    private String hookTypeName;
    private String hookTypeVersion;
    private HookInvocationPoint invocationPoint;
    private String targetName;
    private String targetType;
    private String targetLogicalId;
    private HookTargetModel targetModel;

    public <TargetModelT extends HookTargetModel> TargetModelT getTargetModel(final Class<TargetModelT> clazz) {
        return HookTargetModel.of(this.targetModel, clazz);
    }

    public <TargetModelT extends HookTargetModel> TargetModelT getTargetModel(final TypeReference<TargetModelT> typeReference) {
        return HookTargetModel.of(this.targetModel, typeReference);
    }
}
