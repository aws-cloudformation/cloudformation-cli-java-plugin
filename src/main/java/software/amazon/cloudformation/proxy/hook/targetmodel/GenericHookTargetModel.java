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
package software.amazon.cloudformation.proxy.hook.targetmodel;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * This class is a concrete agnostic target model that is the default
 * implementation used when the type of the Hook's target is not known. Since it
 * is possible for Hooks to support multiple target types, the type of the
 * hook's target is not known until it receives a request during runtime.
 *
 * The properties of the Hook's target are stored internally in a Map which can
 * be accessed using the {@link HookTargetModel} accessor methods
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@JsonDeserialize(as = GenericHookTargetModel.class)
public class GenericHookTargetModel extends HookTargetModel {
    private static final TypeReference<? extends HookTarget> TARGET_REFERENCE = new TypeReference<HookTarget>() {
    };
    private static final TypeReference<GenericHookTargetModel> MODEL_REFERENCE = new TypeReference<GenericHookTargetModel>() {
    };

    @JsonAnySetter
    private void setTargetModelProperties(final String key, final Object value) {
        setTargetModelProperty(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getTargetModelProperties() {
        return super.getTargetModel();
    }

    @Override
    protected HookTargetType getHookTargetType() {
        return HookTargetType.GENERIC;
    }

    @Override
    protected TypeReference<? extends HookTarget> getHookTargetTypeReference() {
        return TARGET_REFERENCE;
    }

    @Override
    protected TypeReference<GenericHookTargetModel> getTargetModelTypeReference() {
        return MODEL_REFERENCE;
    }
}
