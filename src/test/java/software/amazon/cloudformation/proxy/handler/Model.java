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
package software.amazon.cloudformation.proxy.handler;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Set;

@lombok.Builder
@lombok.Data
@lombok.EqualsAndHashCode
@lombok.ToString
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Model {
    public static final String TYPE_NAME = "AWS::Code::Repository";

    @JsonProperty("RepoName")
    private String repoName;
    @JsonProperty("Users")
    private Set<String> users;
    @JsonProperty("Arn")
    private String arn;
    @JsonProperty("Created")
    private Date created;
}
