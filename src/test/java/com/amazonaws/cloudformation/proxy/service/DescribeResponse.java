package com.amazonaws.cloudformation.proxy.service;

import org.joda.time.DateTime;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkField;

import java.util.Collections;
import java.util.List;

@lombok.Getter
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.ToString
public class DescribeResponse extends AwsResponse {
    private final String repoName;
    private final String repoArn;
    private final DateTime createdWhen;
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

    public static class Builder extends BuilderImpl {
        private String repoName;
        private String repoArn;
        private DateTime createdWhen;

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

        public Builder createdWhen(DateTime when) {
            createdWhen = when;
            return this;
        }
    }
}
