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

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.List;
import lombok.Data;
import org.junit.jupiter.api.Test;

public class TypeConverterTest {
    @Data
    public static class ComplexObject {
        @JsonProperty("Key")
        private String key;

        @JsonProperty("Value")
        private String value;
    }

    public static final String LIST_OF_OBJECTS = "[{\"Key\" : \"Key\",\"Value\" :\"Value\"},{\"Key\" : \"Key\",\"Value\":\"Value\"}]";
    public static final String OBJECT = "{\"Key\" : \"Key\",\"Value\" : \"Value\"}";

    final TypeReference<ComplexObject> typeReferenceComplexObject = new TypeReference<ComplexObject>() {
    };
    final TypeReference<List<ComplexObject>> typeReferenceComplexObjectList = new TypeReference<List<ComplexObject>>() {
    };

    @Test
    @SuppressWarnings("unchecked")
    public void castMultiTypePropertyToList() throws IOException {
        final Serializer ser = new Serializer();

        // mimics if multitype was casted to an object
        final Object multiTypeProperty = ser.deserialize(LIST_OF_OBJECTS, new TypeReference<Object>() {
        });

        Object converted = TypeConverter.convertProperty(multiTypeProperty, typeReferenceComplexObject,
            typeReferenceComplexObjectList);

        assertThat(converted).isInstanceOf(List.class);

        List<ComplexObject> complexObject = (List<ComplexObject>) converted;
        assertThat(complexObject.size()).isEqualTo(2);
        assertThat(complexObject.get(0)).isInstanceOf(ComplexObject.class);
        assertThat(complexObject.get(0).key).isEqualTo("Key");
        assertThat(complexObject.get(0).value).isEqualTo("Value");
        assertThat(complexObject.get(1).key).isEqualTo("Key");
        assertThat(complexObject.get(1).value).isEqualTo("Value");
    }

    @Test
    public void castMultiTypePropertyToObject() throws IOException {
        final Serializer ser = new Serializer();

        // mimics if multitype was casted to an object
        final Object multiTypeProperty = ser.deserialize(OBJECT, new TypeReference<Object>() {
        });

        Object converted = TypeConverter.convertProperty(multiTypeProperty, typeReferenceComplexObject,
            typeReferenceComplexObjectList);
        assertThat(converted).isInstanceOf(ComplexObject.class);
        ComplexObject complexObject = (ComplexObject) converted;
        assertThat(complexObject.key).isEqualTo("Key");
        assertThat(complexObject.value).isEqualTo("Value");
    }

    @Test
    public void castMultiTypePropertyWithInvalidReferences() throws IOException {
        final Serializer ser = new Serializer();

        // mimics if multitype was casted to an object
        final Object multiTypeProperty = ser.deserialize(OBJECT, new TypeReference<Object>() {
        });

        try {
            Object converted = TypeConverter.convertProperty(multiTypeProperty, new TypeReference<Integer>() {
            }, new TypeReference<Boolean>() {
            });

        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("No Suitable Type Reference");
        }
    }

}
