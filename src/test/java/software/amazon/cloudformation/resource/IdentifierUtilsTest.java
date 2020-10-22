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
import org.junit.jupiter.api.Test;

public class IdentifierUtilsTest {
    @Test
    public void generateResourceIdentifier_withDefaultLength() {
        String result = IdentifierUtils.generateResourceIdentifier("my-resource", "123456");
        assertThat(result.length()).isLessThanOrEqualTo(40);
        assertThat(result).startsWith("my-resource-");
    }

    @Test
    public void generateResourceIdentifier_withShortLength_floorAt12_prefixDropped() {
        String result = IdentifierUtils.generateResourceIdentifier("my-resource", "123456", 10);

        // the random identity will always be AT LEAST 12 characters long
        assertThat(result.length()).isLessThanOrEqualTo(12);

        // to ensure randomness in the identity, the result will always be a random
        // string PREFIXED by the size of
        // string that is left to fix the max length.
        assertThat(result).doesNotStartWith("my-resource-");
    }

    @Test
    public void generateResourceIdentifier_withShortLength_prefixTruncated() {
        String result = IdentifierUtils.generateResourceIdentifier("my-resource", "123456", 18);
        assertThat(result.length()).isLessThanOrEqualTo(18);

        // to ensure randomness in the identity, the result will always be a random
        // string PREFIXED by the size of
        // string that is left to fix the max length.
        assertThat(result).startsWith("my-re-");
    }

    @Test
    public void generateResourceIdentifier_withStackNameStackId() {
        String result = IdentifierUtils.generateResourceIdentifier("my-stack-name", "my-resource", "123456", 18);
        assertThat(result.length()).isLessThanOrEqualTo(18);

        // to ensure randomness in the identity, the result will always be a random
        // string PREFIXED by the size of
        // string that is left to fix the max length.
        assertThat(result).startsWith("my-my-");
    }

    @Test
    public void generateResourceIdentifier_withStackName() {
        String result = IdentifierUtils.generateResourceIdentifier("my-stack-name", "my-resource", "123456", 50);
        assertThat(result.length()).isLessThanOrEqualTo(49);

        // to ensure randomness in the identity, the result will always be a random
        // string PREFIXED by the size of
        // string that is left to fix the max length.
        assertThat(result).startsWith("my-stack-name-my-resource-");
    }

    @Test
    public void generateResourceIdentifier_withStackNameLessThanPreferredLen() {
        String result = IdentifierUtils.generateResourceIdentifier("my-stack-name", "my-resource", "123456", 16);
        assertThat(result.length()).isLessThanOrEqualTo(16);

        // to ensure randomness in the identity, the result will always be a random
        // string PREFIXED by the size of
        // string that is left to fix the max length.
        assertThat(result).startsWith("mym-");
    }

    @Test
    public void generateResourceIdentifier_withStackNameBothFitMaxLen() {
        String result = IdentifierUtils.generateResourceIdentifier(
            "arn:aws:cloudformation:us-east-1:123456789012:stack/my-stack-name/084c0bd1-082b-11eb-afdc-0a2fadfa68a5",
            "my-resource", "123456", 255);
        assertThat(result.length()).isLessThanOrEqualTo(44);
        assertThat(result).isEqualTo("my-stack-name-my-resource-hDoP0dahAFjd");
    }

    @Test
    public void generateResourceIdentifier_withLongStackNameAndShotLogicalId() {
        String result = IdentifierUtils.generateResourceIdentifier(
            "arn:aws:cloudformation:us-east-1:123456789012:stack/my-very-very-very-very-very-very-long-custom-stack-name/084c0bd1-082b-11eb-afdc-0a2fadfa68a5",
            "abc", "123456", 36);
        assertThat(result.length()).isLessThanOrEqualTo(36);
        assertThat(result).isEqualTo("my-very-very-very-v-abc-hDoP0dahAFjd");
    }

    @Test
    public void generateResourceIdentifier_withShortStackNameAndLongLogicalId() {
        String result = IdentifierUtils.generateResourceIdentifier("abc",
            "my-very-very-very-very-very-very-long-custom-logical-id", "123456", 36);
        assertThat(result.length()).isLessThanOrEqualTo(36);
        assertThat(result).isEqualTo("abc-my-very-very-very-v-hDoP0dahAFjd");
    }

    @Test
    public void generateResourceIdentifier_withLongStackNameAndLongLogicalId() {
        String result = IdentifierUtils.generateResourceIdentifier(
            "arn:aws:cloudformation:us-east-1:123456789012:stack/my-very-very-very-very-very-very-long-custom-stack-name/084c0bd1-082b-11eb-afdc-0a2fadfa68a5",
            "my-very-very-very-very-very-very-long-custom-logical-id", "123456", 36);
        assertThat(result.length()).isEqualTo(36);
        assertThat(result).isEqualTo("my-very-ver-my-very-ver-hDoP0dahAFjd");
    }

    @Test
    public void generateResourceIdentifier_withStackInValidInput() {
        try {
            IdentifierUtils.generateResourceIdentifier("stack/my-stack-name", "my-resource", "123456", 255);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("stack/my-stack-name is not a valid Stack name");
        }
    }

    @Test
    public void generateResourceIdentifier_withStackValidStackId() {
        try {
            IdentifierUtils.generateResourceIdentifier(
                "arn:aws:cloudformation:us-east-1:123456789012:stack/my-stack-name/084c0bd1-082b-11eb-afdc-0a2fadfa68a5",
                "my-resource", "123456", 14);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Cannot generate resource IDs shorter than 15 characters.");
        }
    }
}
