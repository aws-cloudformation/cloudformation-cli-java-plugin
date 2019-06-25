package com.amazonaws.cloudformation.proxy.service;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;

import java.util.Collections;
import java.util.List;

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
    public static class Builder extends BuilderImpl {
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
    }
}
