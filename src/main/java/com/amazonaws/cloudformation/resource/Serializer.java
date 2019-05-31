package com.amazonaws.cloudformation.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Map;

public class Serializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Serializer() {
        configureObjectMapper(this.objectMapper);
    }

    /**
     * Configures the specified ObjectMapper with the (de)serialization behaviours we want gto enforce
     * @param objectMapper
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

    public Map<String, Object> serializeToMap(final Object modelObject) {
        return objectMapper.convertValue(modelObject, new TypeReference<Map<String,Object>>() {});
    }

    public <T> T deserialize(final Object o,
                             final Class<T> valueType) throws IOException {
        return deserialize(objectMapper.writeValueAsString(o), valueType);
    }

    public <T> T deserialize(final String s,
                             final Class<T> valueType) throws IOException {
        return this.objectMapper.readValue(s, valueType);
    }

    public <T> T deserialize(final String s,
                             final TypeReference<?> reference) throws IOException {
        return this.objectMapper.readValue(s, reference);
    }
}
