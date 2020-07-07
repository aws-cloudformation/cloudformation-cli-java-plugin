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
package software.amazon.cloudformation.proxy.service;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.ToString
public class DescribeRequest extends AwsRequest {
    private final String repoName;

    private DescribeRequest(Builder b) {
        super(b);
        repoName = b.repoName;
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    @Override
    public Builder toBuilder() {
        Builder b = new Builder();
        return b.repoName(repoName);
    }

    @lombok.Getter
    @lombok.EqualsAndHashCode(callSuper = true)
    @lombok.ToString(callSuper = true)
    public static class Builder extends BuilderImpl implements SdkPojo {
        private String repoName;

        @Override
        public DescribeRequest build() {
            return new DescribeRequest(this);
        }

        public Builder repoName(String name) {
            this.repoName = name;
            return this;
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration awsRequestOverrideConfig) {
            super.overrideConfiguration(awsRequestOverrideConfig);
            return this;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
