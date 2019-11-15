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
package software.amazon.cloudformation.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

import software.amazon.cloudformation.proxy.aws.AWSServiceSerdeModule;

public class Serializer {

    private static final ObjectMapper OBJECT_MAPPER;

    private static final ObjectMapper STRICT_OBJECT_MAPPER;

    /**
     * Configures the specified ObjectMapper with the (de)serialization behaviours
     * we want gto enforce for strict serialization (for validation purposes)
     *
     * @param objectMapper ObjectMapper instance to configure
     */
    static {
        STRICT_OBJECT_MAPPER = new ObjectMapper();
        STRICT_OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        STRICT_OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        STRICT_OBJECT_MAPPER.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        STRICT_OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        STRICT_OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        STRICT_OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        STRICT_OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        STRICT_OBJECT_MAPPER.registerModule(new AWSServiceSerdeModule());
        STRICT_OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    /**
     * Configures the specified ObjectMapper with the (de)serialization behaviours
     * we want to enforce NOTE: We intend to move towards versioned protocol between
     * caller (CloudFormation) and the various handlers. For now, loose
     * serialization at the protocol layer allows some flexibility between these
     * components.
     *
     * @param objectMapper ObjectMapper instance to configure
     */
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.registerModule(new AWSServiceSerdeModule());
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public <T> String serialize(final T modelObject) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(modelObject);
    }

    public <T> T deserialize(final String s, final TypeReference<?> reference) throws IOException {
        return OBJECT_MAPPER.readValue(s, reference);
    }

    public <T> T deserializeStrict(final String s, final TypeReference<?> reference) throws IOException {
        return STRICT_OBJECT_MAPPER.readValue(s, reference);
    }
}
