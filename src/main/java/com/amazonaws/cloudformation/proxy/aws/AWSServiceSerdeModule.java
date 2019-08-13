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
package com.amazonaws.cloudformation.proxy.aws;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.ser.Serializers;

import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class AWSServiceSerdeModule extends Module {

    public static class AWSSerializers extends Serializers.Base {

        @Override
        public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
            if (SdkPojo.class.isAssignableFrom(type.getRawClass()) && !SdkBuilder.class.isAssignableFrom(type.getRawClass())) {
                return new SdkPojoSerializer();
            }
            return null;
        }
    }

    public static class AWSDeserializers extends Deserializers.Base {

        @Override
        public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc)
            throws JsonMappingException {
            if (SdkPojo.class.isAssignableFrom(type.getRawClass()) && !SdkBuilder.class.isAssignableFrom(type.getRawClass())) {
                return new SdkPojoDeserializer(type);
            }
            return null;
        }
    }

    @Override
    public String getModuleName() {
        return "AWSServiceSerializationDeserialization";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new AWSSerializers());
        context.addDeserializers(new AWSDeserializers());
    }
}
