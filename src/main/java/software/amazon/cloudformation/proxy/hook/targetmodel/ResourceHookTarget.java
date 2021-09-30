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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import software.amazon.cloudformation.resource.Serializer;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public abstract class ResourceHookTarget implements HookTarget {

    @JsonIgnore
    protected JSONObject targetSchema;

    @JsonIgnore
    private Map<String, Object> properties;

    @JsonIgnore
    private Map<String, Object> additionalProperties;

    @Override
    public final Object get(final String key) {
        return MapUtils.getObject(getProperties(), key);
    }

    public final Object getOrDefault(final String key, final Object defaultValue) {
        return MapUtils.getObject(getProperties(), key, defaultValue);
    }

    public final boolean hasProperty(final String key) {
        return MapUtils.emptyIfNull(getProperties()).containsKey(key);
    }

    public final Map<String, Object> getProperties() {
        if (properties == null) {
            properties = new Serializer().convert(this, Serializer.MAP_TYPE_REFERENCE);
        }

        return ImmutableMap.copyOf(properties);
    }

    @JsonAnyGetter
    public final Map<String, Object> getAdditionalProperties() {
        if (additionalProperties == null) {
            additionalProperties = new HashMap<>();
        }

        return ImmutableMap.copyOf(additionalProperties);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Called internally during deserialization")
    @JsonAnySetter
    private void setAdditionalProperty(final String key, final Object value) {
        if (BooleanUtils.or(ArrayUtils.toArray(isCloudFormationRegistryType(), hasDefinedSchema()))) {
            return;
        }

        if (additionalProperties == null) {
            additionalProperties = new HashMap<>();
        }
        additionalProperties.put(key, value);
    }

    @Override
    public final HookTargetType getHookTargetType() {
        return HookTargetType.RESOURCE;
    }

    public abstract JSONObject getPrimaryIdentifier();

    public abstract List<JSONObject> getAdditionalIdentifiers();

    public abstract JSONObject targetSchemaJSONObject();

    public abstract Boolean hasDefinedSchema();

    public abstract Boolean isCloudFormationRegistryType();

    protected JSONObject loadSchema(final String schema) {
        return loadSchema(schema, this.getClass());
    }

    @VisibleForTesting
    static JSONObject loadSchema(final String schema, final Class<?> clazz) {
        if (StringUtils.isBlank(schema) || StringUtils.equalsIgnoreCase(schema, "null")) {
            return new JSONObject(JSONObject.NULL);
        }

        final String targetSchema = StringUtils.trim(schema);
        try {
            final String json;
            if (StringUtils.startsWith(targetSchema, "{") && StringUtils.endsWith(targetSchema, "}")) {
                json = targetSchema;
            } else if (StringUtils.endsWith(targetSchema, ".json")) {
                final InputStream is = Optional.ofNullable(clazz.getClassLoader().getResourceAsStream(targetSchema))
                    .orElse(clazz.getResourceAsStream(targetSchema));
                json = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            } else {
                json = null;
            }

            return StringUtils.isNotBlank(json) ? new JSONObject(new JSONTokener(json)) : new JSONObject(JSONObject.NULL);
        } catch (IOException | RuntimeException e) {
            return new JSONObject(JSONObject.NULL);
        }
    }
}
