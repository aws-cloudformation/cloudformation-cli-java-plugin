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
package software.amazon.cloudformation.exceptions;

/**
 * Uses for this exception class should delegate instead to
 * CfnUnsupportedTargetException as it maps to UnsupportedTarget error code.
 * This deprecated exception maps to an InvalidRequest error code. Keeping the
 * same for backwards-compatibility
 */
public class UnsupportedTargetException extends CfnInvalidRequestException {

    private static final long serialVersionUID = -1646136434112354328L;
    private static final String ERROR_MESSAGE = "Unsupported target: [%s]";

    public UnsupportedTargetException(final Throwable cause) {
        super(cause);
    }

    public UnsupportedTargetException(final String targetTypeName) {
        this(targetTypeName, null);
    }

    public UnsupportedTargetException(final String targetTypeName,
                                      final Throwable cause) {
        super(String.format(ERROR_MESSAGE, targetTypeName), cause);
    }
}
