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
import java.util.Date;
import java.util.List;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.ToString
public class DescribeResponse extends AwsResponse {
    private final String repoName;
    private final String repoArn;
    private final Date createdWhen;

    private DescribeResponse(Builder b) {
        super(b);
        repoName = b.repoName;
        repoArn = b.repoArn;
        createdWhen = b.createdWhen;
    }

    @Override
    public Builder toBuilder() {
        return new Builder().createdWhen(createdWhen).repoArn(repoArn).repoName(repoName);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    public static class Builder extends BuilderImpl implements SdkPojo {
        private String repoName;
        private String repoArn;
        private Date createdWhen;

        @Override
        public DescribeResponse build() {
            return new DescribeResponse(this);
        }

        public Builder repoName(String name) {
            this.repoName = name;
            return this;
        }

        public Builder repoArn(String arn) {
            repoArn = arn;
            return this;
        }

        public Builder createdWhen(Date when) {
            createdWhen = when;
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
