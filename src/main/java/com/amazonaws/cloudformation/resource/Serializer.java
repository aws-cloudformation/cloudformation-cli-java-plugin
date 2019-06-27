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
package com.amazonaws.cloudformation.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import org.json.JSONObject;

public class Serializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Serializer() {
        configureObjectMapper(this.objectMapper);
    }

    /**
     * Configures the specified ObjectMapper with the (de)serialization behaviours
     * we want gto enforce
     *
     * @param objectMapper ObjectMapper instance to configure
     */
    private void configureObjectMapper(final ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public <T> JSONObject serialize(final T modelObject) throws JsonProcessingException {
        if (modelObject instanceof JSONObject) {
            return (JSONObject) modelObject;
        }

        return new JSONObject(objectMapper.writeValueAsString(modelObject));
    }

    public <T> T deserialize(final String s, final TypeReference<?> reference) throws IOException {
        return this.objectMapper.readValue(s, reference);
    }
}
