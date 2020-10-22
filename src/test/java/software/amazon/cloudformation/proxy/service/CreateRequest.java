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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.builder.SdkBuilder;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.ToString(callSuper = true)
public class CreateRequest extends AwsRequest {

    private static final SdkField<String> REPO_NAME_FIELD = SdkField.<String>builder(MarshallingType.STRING)
        .getter((obj) -> ((CreateRequest) obj).repoName).setter((obj, val) -> ((Builder) obj).repoName(val))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("repoName").build()).build();

    private static final SdkField<String> USER_NAME_FIELD = SdkField.<String>builder(MarshallingType.STRING)
        .getter((obj) -> ((CreateRequest) obj).getUserName()).setter((obj, val) -> ((Builder) obj).repoName(val))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userName").build()).build();

    private static final List<
        SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(REPO_NAME_FIELD, USER_NAME_FIELD));

    private final String repoName;
    private final String userName;

    private CreateRequest(Builder b) {
        super(b);
        this.repoName = b.repoName;
        this.userName = b.userName;
    }

    @Override
    public Builder toBuilder() {
        Builder b = new Builder();
        return b.userName(userName).repoName(repoName);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    @lombok.Getter
    @lombok.EqualsAndHashCode(callSuper = true)
    @lombok.ToString(callSuper = true)
    public static class Builder extends BuilderImpl implements SdkPojo, SdkBuilder<Builder, CreateRequest> {
        private String repoName;
        private String userName;

        @Override
        public CreateRequest build() {
            return new CreateRequest(this);
        }

        public Builder repoName(String name) {
            this.repoName = name;
            return this;
        }

        public Builder userName(String name) {
            this.userName = name;
            return this;
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration awsRequestOverrideConfig) {
            super.overrideConfiguration(awsRequestOverrideConfig);
            return this;
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
