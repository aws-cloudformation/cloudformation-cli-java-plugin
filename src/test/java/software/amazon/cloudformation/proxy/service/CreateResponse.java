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
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.builder.SdkBuilder;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.ToString
public class CreateResponse extends AwsResponse {

    private static final SdkField<String> REPO_NAME_FIELD = SdkField.<String>builder(MarshallingType.STRING)
        .getter((obj) -> ((CreateResponse) obj).getRepoName()).setter((obj, val) -> ((CreateResponse.Builder) obj).repoName(val))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("repoName").build()).build();

    private static final SdkField<String> ERROR_FIELD = SdkField.<String>builder(MarshallingType.STRING)
        .getter((obj) -> ((CreateResponse) obj).getError()).setter((obj, val) -> ((Builder) obj).error(val))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userName").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(REPO_NAME_FIELD, ERROR_FIELD));

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

    public static class Builder extends BuilderImpl implements SdkPojo, SdkBuilder<Builder, CreateResponse> {
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

        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
