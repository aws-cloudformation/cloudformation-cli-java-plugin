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

import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class CfnNotStabilizedException extends BaseHandlerException {

    private static final long serialVersionUID = -1646136434112354328L;
    private static final HandlerErrorCode ERROR_CODE = HandlerErrorCode.NotStabilized;

    public CfnNotStabilizedException(final Throwable cause) {
        super(cause, ERROR_CODE);
    }

    /**
     * @param resourceTypeName
     * @param resourceIdentifier
     * @param reason Reason why the resource did not stabilize. This should include the current and
     *               desired state.
     *
     *               Example: "Exceeded retry limit for DescribeResourceStatus.
     *               Current Value: IN_PROGRESS. Desired Value: COMPLETE."
     */
    public CfnNotStabilizedException(final String resourceTypeName,
                                     final String resourceIdentifier,
                                     final String reason) {
        this(resourceTypeName, resourceIdentifier, reason, null);
    }

    /**
     * @param resourceTypeName
     * @param resourceIdentifier
     * @param reason Reason why the resource did not stabilize. This should include the current and
     *               desired state.
     *
     *               Example: "Exceeded retry limit for DescribeResourceStatus.
     *               Current Value: IN_PROGRESS. Desired Value: COMPLETE."
     * @param cause
     */
    public CfnNotStabilizedException(final String resourceTypeName,
                                     final String resourceIdentifier,
                                     final String reason,
                                     final Throwable cause) {
        super(String.format(ERROR_CODE.getMessage(), resourceTypeName, resourceIdentifier, reason),
            cause, ERROR_CODE);
    }

    /**
     * use {@link #CfnNotStabilizedException(String, String, String)}
     *
     * @param resourceTypeName
     * @param resourceIdentifier
     */
    @Deprecated
    public CfnNotStabilizedException(final String resourceTypeName,
                                     final String resourceIdentifier) {
        this(resourceTypeName, resourceIdentifier, (Throwable) null);
    }

    /**
     * use {@link #CfnNotStabilizedException(String, String, String, Throwable)}
     *
     * @param resourceTypeName
     * @param resourceIdentifier
     * @param cause
     */
    @Deprecated
    public CfnNotStabilizedException(final String resourceTypeName,
                                     final String resourceIdentifier,
                                     final Throwable cause) {
        this(resourceTypeName, resourceIdentifier, "", cause);
    }
}
