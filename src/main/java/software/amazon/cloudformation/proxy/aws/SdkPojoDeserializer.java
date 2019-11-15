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
package software.amazon.cloudformation.proxy.aws;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.utils.builder.SdkBuilder;

@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class SdkPojoDeserializer extends StdDeserializer<SdkPojo> {

    private static final long serialVersionUID = -1L;

    private final JavaType type;

    public SdkPojoDeserializer(JavaType type) {
        super(type);
        this.type = type;
    }

    @Override
    public JavaType getValueType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SdkPojo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Class<? extends SdkPojo> kclass = (Class<? extends SdkPojo>) type.getRawClass();
        SdkPojo builder = createBuilder(p, kclass);
        return readPojo(builder, p, ctxt);
    }

    private SdkPojo readPojo(final SdkPojo pojo, JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.isExpectedStartObjectToken()) {
            throw new JsonMappingException(p, "Expected to be in START_OBJECT got " + p.currentToken());
        }

        Map<String, SdkField<?>> fieldMap = getFields(pojo);
        JsonToken next = p.nextToken();
        ObjectMapper codec = (ObjectMapper) p.getCodec();
        while (next != JsonToken.END_OBJECT) {
            /*
             * if (next != JsonToken.FIELD_NAME) { throw new JsonMappingException(p,
             * "Expecting to get FIELD_NAME token, got " + next); }
             */
            String fieldName = p.getCurrentName();
            SdkField<?> sdkField = fieldMap.get(fieldName);
            if (sdkField == null) {
                if (codec.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
                    throw new JsonMappingException(p, "Unknown property encountered " + fieldName);
                }
                // we need to skip this
                next = p.nextToken();
                if (next == JsonToken.START_ARRAY || next == JsonToken.START_OBJECT) {
                    p.skipChildren();
                }
                // all others, just proceed to next token
                next = p.nextToken();
                continue;
            }
            // progress to next thing to read
            p.nextToken();
            // Okay we need to parse the field and set it
            Object value = readObject(sdkField, p, ctxt);
            sdkField.set(pojo, value);
            next = p.nextToken();
        }
        return pojo instanceof SdkBuilder ? (SdkPojo) ((SdkBuilder) pojo).build() : pojo;
    }

    private Object readObject(SdkField<?> field, JsonParser p, DeserializationContext ctxt) throws IOException {

        MarshallingType<?> type = field.marshallingType();
        switch (p.currentToken()) {
            case VALUE_FALSE:
            case VALUE_TRUE: {
                if (type.equals(MarshallingType.BOOLEAN)) {
                    return p.getBooleanValue();
                }
                throw new JsonMappingException(p, "Type mismatch, expecting " + type + " got boolean field value");
            }

            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT: {
                if (type.equals(MarshallingType.INTEGER)) {
                    return p.getIntValue();
                } else if (type.equals(MarshallingType.LONG)) {
                    return p.getLongValue();
                } else if (type.equals(MarshallingType.FLOAT)) {
                    return p.getFloatValue(); // coerce should work
                } else if (type.equals(MarshallingType.DOUBLE)) {
                    return p.getDoubleValue(); // coerce should work
                } else if (type.equals(MarshallingType.BIG_DECIMAL)) {
                    return p.getDecimalValue(); // coerce should work
                } else if (type.equals(MarshallingType.INSTANT)) { // we serialize as BigDecimals
                    JsonDeserializer<Object> deser = ctxt.findRootValueDeserializer(ctxt.constructType(Instant.class));
                    return deser.deserialize(p, ctxt);
                }
                throw new JsonMappingException(p,
                                               "Type mismatch, expecting " + type + " got int/float/double/big_decimal/instant");
            }

            case VALUE_STRING: {
                if (type.equals(MarshallingType.STRING)) {
                    return p.getText();
                } else if (type.equals(MarshallingType.SDK_BYTES)) {
                    byte[] bytes = p.getBinaryValue();
                    return SdkBytes.fromByteArray(bytes);
                }
                throw new JsonMappingException(p, "Type mismatch, expecting " + type + " got string/bytes");
            }

            case START_OBJECT: {
                if (type.equals(MarshallingType.MAP)) {
                    return readMap(field, p, ctxt);
                } else if (type.equals(MarshallingType.SDK_POJO)) {
                    return readPojo(field.constructor().get(), p, ctxt);
                }
                throw new JsonMappingException(p, "Type mismatch, expecting " + type + " got Map/SdkPojo");
            }

            case START_ARRAY: {
                if (type.equals(MarshallingType.LIST)) {
                    return readList(field, p, ctxt);
                }
                throw new JsonMappingException(p, "Type mismatch, expecting " + type + " got List type");
            }

            case VALUE_NULL:
                return null;

            default:
                throw new JsonMappingException(p, "Can not map type " + type + " Token = " + p.currentToken());
        }
    }

    private Map<String, Object> readMap(SdkField<?> field, JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.isExpectedStartObjectToken()) {
            throw new JsonMappingException(p, "Expected start of object token for Map got " + p.currentToken());
        }

        Map<String, Object> value = new LinkedHashMap<>();
        MapTrait trait = field.getTrait(MapTrait.class);
        SdkField<?> valueType = trait.valueFieldInfo();
        while (p.nextToken() != JsonToken.END_OBJECT) {
            if (p.currentToken() != JsonToken.FIELD_NAME) {
                throw new JsonMappingException(p, "Expecting String key for map got " + p.currentToken());
            }
            String fieldName = p.getCurrentName();
            // progress to next token
            p.nextToken();
            value.put(fieldName, readObject(valueType, p, ctxt));
        }
        return value;
    }

    private List<Object> readList(SdkField<?> field, JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.isExpectedStartArrayToken()) {
            throw new JsonMappingException(p, "Expecting array start token got " + p.currentToken());
        }

        ListTrait trait = field.getTrait(ListTrait.class);
        SdkField<?> valueType = trait.memberFieldInfo();
        List<Object> value = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            value.add(readObject(valueType, p, ctxt));
        }
        return value;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    private SdkPojo createBuilder(JsonParser parser, Class<? extends SdkPojo> kclass) throws IOException {
        try {
            Method method = kclass.getMethod("builder");
            return (SdkPojo) method.invoke(kclass);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new JsonMappingException(parser, "Could not create builder for SdkPojo " + kclass, e);
        }
    }

    private Map<String, SdkField<?>> getFields(SdkPojo pojo) {
        List<SdkField<?>> sdkFields = pojo.sdkFields();
        Map<String, SdkField<?>> fields = new HashMap<>(sdkFields.size());
        for (SdkField<?> each : sdkFields) {
            fields.put(each.locationName(), each);
        }
        return fields;
    }
}
