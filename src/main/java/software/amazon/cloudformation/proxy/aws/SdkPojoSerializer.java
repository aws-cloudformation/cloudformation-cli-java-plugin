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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.MapTrait;

@lombok.EqualsAndHashCode(callSuper = true)
@lombok.ToString
public class SdkPojoSerializer extends StdSerializer<SdkPojo> {

    private static final long serialVersionUID = -1L;

    public SdkPojoSerializer() {
        super(SdkPojo.class);
    }

    @Override
    public void serialize(SdkPojo value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        writeSdkPojo(value, gen, serializers);
    }

    @SuppressWarnings("unchecked")
    private void writeObject(Object value, SdkField<?> sdkField, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        MarshallingType<?> type = sdkField.marshallingType();
        if (type.equals(MarshallingType.BOOLEAN)) {
            gen.writeBoolean((Boolean) value);
        } else if (type.equals(MarshallingType.DOUBLE)) {
            gen.writeNumber((Double) value);
        } else if (type.equals(MarshallingType.INTEGER)) {
            gen.writeNumber((Integer) value);
        } else if (type.equals(MarshallingType.FLOAT)) {
            gen.writeNumber((Float) value);
        } else if (type.equals(MarshallingType.STRING)) {
            gen.writeString((String) value);
        } else if (type.equals(MarshallingType.BIG_DECIMAL)) {
            gen.writeNumber((BigDecimal) value);
        } else if (type.equals(MarshallingType.SDK_BYTES)) {
            gen.writeBinary(((SdkBytes) value).asByteArray());
        } else if (type.equals(MarshallingType.INSTANT)) {
            JsonSerializer<Object> serializer = serializers.findValueSerializer(Instant.class);
            serializer.serialize(value, gen, serializers);
        } else if (type.equals(MarshallingType.LONG)) {
            gen.writeNumber((Long) value);
        } else if (type.equals(MarshallingType.SDK_POJO)) {
            writeSdkPojo((SdkPojo) value, gen, serializers);
        } else if (type.equals(MarshallingType.LIST)) {
            writeSdkList((Collection<Object>) value, sdkField, gen, serializers);
        } else if (type.equals(MarshallingType.MAP)) {
            writeSdkMap((Map<String, Object>) value, sdkField, gen, serializers);
        }
    }

    private void writeSdkPojo(SdkPojo value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        JsonInclude.Value propertyInclusion = serializers.getConfig().getDefaultPropertyInclusion();
        boolean nonNull = propertyInclusion.getValueInclusion().equals(JsonInclude.Include.NON_NULL);

        gen.writeStartObject();
        for (SdkField<?> sdkField : value.sdkFields()) {
            String fieldName = sdkField.locationName();
            Object fieldValue = sdkField.getValueOrDefault(value);
            if (nonNull && fieldValue == null) {
                continue;
            }
            gen.writeFieldName(fieldName);
            writeObject(fieldValue, sdkField, gen, serializers);
        }
        gen.writeEndObject();
    }

    private void
        writeSdkList(Collection<Object> collection, SdkField<?> sdkField, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (collection == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartArray();
        ListTrait trait = sdkField.getTrait(ListTrait.class);
        SdkField<?> inner = trait.memberFieldInfo();
        for (Object each : collection) {
            writeObject(each, inner, gen, serializers);
        }
        gen.writeEndArray();
    }

    private void writeSdkMap(Map<String, Object> map, SdkField<?> sdkField, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
        if (map == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();
        MapTrait trait = sdkField.getTrait(MapTrait.class);
        SdkField<?> valueType = trait.valueFieldInfo();
        for (Map.Entry<String, Object> each : map.entrySet()) {
            gen.writeFieldName(each.getKey());
            Object value = each.getValue();
            writeObject(value, valueType, gen, serializers);
        }
        gen.writeEndObject();
    }
}
