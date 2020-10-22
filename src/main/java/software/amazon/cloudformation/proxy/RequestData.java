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
package software.amazon.cloudformation.proxy;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestData<ResourceT> {
    private Credentials callerCredentials;
    private Credentials providerCredentials;
    private String providerLogGroupName;
    private String logicalResourceId;
    private ResourceT resourceProperties;
    private ResourceT previousResourceProperties;
    private Map<String, String> systemTags;
    private Map<String, String> previousSystemTags;
    private Map<String, String> stackTags;
    private Map<String, String> previousStackTags;
}
