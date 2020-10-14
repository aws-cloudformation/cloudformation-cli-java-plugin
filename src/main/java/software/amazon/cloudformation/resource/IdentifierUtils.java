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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RandomStringUtils;

public class IdentifierUtils {

    private static final int GENERATED_PHYSICALID_MAXLEN = 40;
    private static final int GUID_LENGTH = 12;
    private static final int MIN_PHYSICAL_RESOURCE_ID_LENGTH = 15;
    private static final int MIN_PREFERRED_LENGTH = 17;
    private static final Splitter STACKID_SPLITTER = Splitter.on('/');
    private static final Pattern STACK_ARN_PATTERN = Pattern.compile("^[a-z0-9-:]*stack/[-a-z0-9A-Z/]*");
    private static final Pattern STACK_NAME_PATTERN = Pattern.compile("^[-a-z0-9A-Z]*");

    private IdentifierUtils() {
    }

    /**
     * For named resources, use this method to safely generate a user friendly
     * resource name when the customer does not pass in an explicit name For more
     * info, see the named resources section of the developer guide https://...
     *
     * @param logicalResourceId logical name for the resource as defined in
     *            CloudFormation
     * @param clientRequestToken the idempotent token from CloudFormation to help
     *            detect duplicate calls
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
     * @param logicalResourceId logical name for the resource as defined in
     *            CloudFormation
     * @param clientRequestToken the idempotent token from CloudFormation to help
     *            detect duplicate calls
     * @param maxLength the maximum length size for the identifier
     * @return generated ID string
     */
    public static String
        generateResourceIdentifier(final String logicalResourceId, final String clientRequestToken, final int maxLength) {
        int maxLogicalIdLength = maxLength - (GUID_LENGTH + 1);

        int endIndex = Math.min(logicalResourceId.length(), maxLogicalIdLength);

        StringBuilder sb = new StringBuilder();
        if (endIndex > 0) {
            sb.append(logicalResourceId.substring(0, endIndex)).append("-");
        }

        return sb.append(RandomStringUtils.random(GUID_LENGTH, 0, 0, true, true, null, new Random(clientRequestToken.hashCode())))
            .toString();
    }

    public static String generateResourceIdentifier(final String stackId,
                                                    final String logicalResourceId,
                                                    final String clientRequestToken,
                                                    final int maxLength) {

        if (maxLength < MIN_PHYSICAL_RESOURCE_ID_LENGTH) {
            throw new IllegalArgumentException("Cannot generate resource IDs shorter than " + MIN_PHYSICAL_RESOURCE_ID_LENGTH
                + " characters.");
        }

        String stackName = stackId;

        if (isStackArn(stackId)) {
            stackName = STACKID_SPLITTER.splitToList(stackId).get(1);
        }

        if (!isValidStackName(stackName)) {
            throw new IllegalArgumentException(String.format("%s is not a valid Stack name", stackName));
        }

        // some services don't allow leading dashes. Since stack name is first, clean
        // off any + no consecutive dashes

        final String cleanStackName = stackName.replaceFirst("^-+", "").replaceAll("-{2,}", "-");

        final boolean separate = maxLength > MIN_PREFERRED_LENGTH;
        // 13 char length is reserved for the hashed value and one
        // for each dash separator (if needed). the rest if the characters
        // will get allocated evenly between the stack and resource names

        final int freeCharacters = maxLength - 13 - (separate ? 1 : 0);
        final int[] requestedLengths = new int[2];

        requestedLengths[0] = cleanStackName.length();
        requestedLengths[1] = logicalResourceId.length();

        final int[] availableLengths = fairSplit(freeCharacters, requestedLengths);
        final int charsForStackName = availableLengths[0];
        final int charsForResrcName = availableLengths[1];

        final StringBuilder prefix = new StringBuilder();

        prefix.append(cleanStackName, 0, charsForStackName);
        if (separate) {
            prefix.append("-");
        }
        prefix.append(logicalResourceId, 0, charsForResrcName);

        return IdentifierUtils.generateResourceIdentifier(prefix.toString(), clientRequestToken, maxLength);
    }

    private static boolean isStackArn(String stackId) {
        return STACK_ARN_PATTERN.matcher(stackId).matches() && Iterables.size(STACKID_SPLITTER.split(stackId)) == 3;
    }

    private static boolean isValidStackName(String stackName) {
        return STACK_NAME_PATTERN.matcher(stackName).matches();
    }

    private static int[] fairSplit(final int cap, final int[] buckets) {
        int remaining = cap;

        int[] allocated = new int[buckets.length];
        Arrays.fill(allocated, 0);

        while (remaining > 0) {
            // at least one capacity unit
            int maxAllocation = remaining < buckets.length ? 1 : remaining / buckets.length;

            int bucketSatisfied = 0; // reset on each cap

            for (int i = -1; ++i < buckets.length;) {
                if (allocated[i] < buckets[i]) {
                    final int incrementalAllocation = Math.min(maxAllocation, buckets[i] - allocated[i]);
                    allocated[i] += incrementalAllocation;
                    remaining -= incrementalAllocation;
                } else {
                    bucketSatisfied++;
                }

                if (remaining <= 0 || bucketSatisfied == buckets.length) {
                    return allocated;
                }
            }
        }
        return allocated;
    }
}
