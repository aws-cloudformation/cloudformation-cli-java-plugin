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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;

public class TypeConverter {
    protected TypeConverter() {
        throw new UnsupportedOperationException();
    }

    public static Object convertProperty(final Object input, final TypeReference<?>... typeReferences) // preserves order
    {
        try {
            final Serializer serializer = new Serializer();
            final String stringified_input = serializer.serialize(input);

            int iter = 0;
            while (true) {
                TypeReference<?> typeReference = typeReferences[iter];
                try {
                    return serializer.deserialize(stringified_input, typeReference);
                } catch (JsonProcessingException exception) {
                    if (iter < typeReferences.length - 1) {
                        iter++;
                    } else {
                        throw new RuntimeException("No Suitable Type Reference");
                    }
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Invalid Object to Stringify");
        }
    }
}
