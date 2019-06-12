package com.amazonaws.cloudformation.proxy.service;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.SdkField;

import java.util.Collections;
import java.util.List;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.ToString
public class DescribeRequest extends BaseRequest {
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
        return build(b).repoName(repoName);
    }

    public static class Builder extends BaseRequest.BaseRequestBuilder {
        private String repoName;
        private Boolean fail;
        @Override
        public DescribeRequest build() {
            return new DescribeRequest(this);
        }

        public Builder repoName(String name) {
            this.repoName = name;
            return this;
        }
    }
}
