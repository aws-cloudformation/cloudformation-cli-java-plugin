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
package com.amazonaws.cloudformation.exceptions;

public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = -1646136434112354328L;

    public ResourceAlreadyExistsException(final Throwable cause) {
        super(null, cause);
    }

    public ResourceAlreadyExistsException(final String resourceTypeName,
                                          final String resourceIdentifier) {
        this(resourceTypeName, resourceIdentifier, null);
    }

    public ResourceAlreadyExistsException(final String resourceTypeName,
                                          final String resourceIdentifier,
                                          final Throwable cause) {
        super(String.format("Resource of type '%s' with identifier '%s' already exists.", resourceTypeName, resourceIdentifier),
              cause);
    }
}
