package com.amazonaws.cloudformation.proxy.service;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;

import java.util.Collections;
import java.util.List;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = false)
@lombok.ToString(callSuper = true)
public class CreateRequest extends AwsRequest {

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
    public static class Builder extends BuilderImpl {
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
    }
}
