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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(as = TestResourceHookTargetModel.class)
public class TestResourceHookTargetModel extends ResourceHookTargetModel<TestResource> {

    public TestResourceHookTargetModel(final TestResource resourceProperties) {
        this(resourceProperties, null);
    }

    public TestResourceHookTargetModel(final TestResource resourceProperties,
                                       final TestResource previousResourceProperties) {
        super(resourceProperties, previousResourceProperties);
    }

    @Override
    @JsonIgnore
    public TypeReference<TestResource> getHookTargetTypeReference() {
        return new TypeReference<TestResource>() {
        };
    }

    @Override
    @JsonIgnore
    public TypeReference<TestResourceHookTargetModel> getTargetModelTypeReference() {
        return new TypeReference<TestResourceHookTargetModel>() {
        };
    }
}
