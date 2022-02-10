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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import org.json.JSONObject;

/**
 * This test class represents a resource where the resource schema was not
 * provided at build time.
 */
public class GenericTestResource extends ResourceHookTarget {
    @Override
    public JSONObject getPrimaryIdentifier() {
        return null;
    }

    @Override
    public List<JSONObject> getAdditionalIdentifiers() {
        return null;
    }

    @Override
    public JSONObject targetSchemaJSONObject() {
        return null;
    }

    @Override
    public Boolean hasDefinedSchema() {
        return false;
    }

    @JsonIgnore
    @Override
    public Boolean isCloudFormationRegistryType() {
        return false;
    }
}
