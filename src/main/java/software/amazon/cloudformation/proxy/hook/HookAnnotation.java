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
package software.amazon.cloudformation.proxy.hook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HookAnnotation {
    /**
     * The name of the annotation; this is mandatory.
     */
    private String annotationName;

    /**
     * The status for the hook annotation: this is mandatory.
     */
    private HookAnnotationStatus status;

    /**
     * The optional status message for the annotation.
     */
    private String statusMessage;

    /**
     * The optional remediation message for the annotation.
     */
    private String remediationMessage;

    /**
     * The optional remediation link for the annotation.
     */
    private String remediationLink;

    /**
     * The optional severity level for the annotation.
     */
    private HookAnnotationSeverityLevel severityLevel;

}
