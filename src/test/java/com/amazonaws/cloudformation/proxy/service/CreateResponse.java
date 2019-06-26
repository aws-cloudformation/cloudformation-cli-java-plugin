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
package com.amazonaws.cloudformation.proxy.service;

import java.util.Collections;
import java.util.List;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkField;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.ToString
public class CreateResponse extends AwsResponse {
    private final String repoName;
    private final String error;

    private CreateResponse(Builder b) {
        super(b);
        repoName = b.repoName;
        error = b.error;
    }

    @Override
    public Builder toBuilder() {
        return new Builder().repoName(repoName).error(error);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    public static class Builder extends BuilderImpl {
        private String repoName;
        private String error;

        @Override
        public CreateResponse build() {
            return new CreateResponse(this);
        }

        public Builder repoName(String name) {
            this.repoName = name;
            return this;
        }

        public Builder error(String name) {
            this.error = name;
            return this;
        }
    }
}
