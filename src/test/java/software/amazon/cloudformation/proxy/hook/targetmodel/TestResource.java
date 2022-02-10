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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONObject;

@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class TestResource extends ResourceHookTarget {
    @JsonIgnore
    public static final String TYPE_NAME = "Test::Resource::Type";

    @JsonProperty("Property1")
    private String property1;
    @JsonProperty("Property2")
    private Integer property2;
    @JsonProperty("Tags")
    private Map<String, String> tags;

    @Override
    @JsonIgnore
    public JSONObject getPrimaryIdentifier() {
        return null;
    }

    @Override
    @JsonIgnore
    public List<JSONObject> getAdditionalIdentifiers() {
        return null;
    }

    @Override
    public JSONObject targetSchemaJSONObject() {
        if (targetSchema == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Property1", property1);
            jsonObject.put("Property1", property1);
            jsonObject.put("Tags", tags);
            targetSchema = loadSchema(jsonObject.toString());
        }

        return targetSchema;
    }

    @Override
    public Boolean hasDefinedSchema() {
        return true;
    }

    @JsonIgnore
    @Override
    public Boolean isCloudFormationRegistryType() {
        return true;
    }
}
