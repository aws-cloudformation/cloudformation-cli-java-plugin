/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the &quot;License&quot;).
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the &quot;license&quot; file accompanying this file. This file is distributed
* on an &quot;AS IS&quot; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.resource;

import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;

public class IdentifierUtils {

    private static final int GENERATED_PHYSICALID_MAXLEN = 40;
    private static final int GUID_LENGTH = 12;

    private IdentifierUtils() {
    }

    /**
     * For named resources, use this method to safely generate a user friendly
     * resource name when the customer does not pass in an explicit name For more
     * info, see the named resources section of the developer guide https://...
     *
     * @return generated ID string
     */
    public static String generateResourceIdentifier(final String logicalResourceId, final String clientRequestToken) {
        return generateResourceIdentifier(logicalResourceId, clientRequestToken, GENERATED_PHYSICALID_MAXLEN);
    }

    /**
     * For named resources, use this method to safely generate a user friendly
     * resource name when the customer does not pass in an explicit name For more
     * info, see the named resources section of the developer guide https://...
     *
     * @return generated ID string
     */
    public static String
           generateResourceIdentifier(final String logicalResourceId, final String clientRequestToken, final int maxLength) {
        int maxLogicalIdLength = maxLength - (GUID_LENGTH + 1);

        int endIndex = logicalResourceId.length() > maxLogicalIdLength ? maxLogicalIdLength : logicalResourceId.length();

        StringBuilder sb = new StringBuilder();
        if (endIndex > 0) {
            sb.append(logicalResourceId.substring(0, endIndex)).append("-");
        }

        return sb.append(RandomStringUtils.random(GUID_LENGTH, 0, 0, true, true, null, new Random(clientRequestToken.hashCode())))
            .toString();
    }
}
