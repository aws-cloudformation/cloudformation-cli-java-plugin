package com.amazonaws.cloudformation.proxy.service;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.SdkField;

import java.util.Collections;
import java.util.List;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.ToString
public abstract class BaseRequest extends AwsRequest {
    private final boolean throwAccessDenied;
    private final boolean throwThrottleException;
    protected BaseRequest(Builder b) {
        super(b);
        BaseRequestBuilder builder = (BaseRequestBuilder) b;
        throwAccessDenied = builder.throwAccessDenied;
        throwThrottleException = builder.throwThrottleException;
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    protected <T extends BaseRequestBuilder> T build(T builder) {
        builder.throwAccessDenied(throwAccessDenied).throwThrottleException(throwThrottleException);
        return builder;
    }

    protected static abstract class BaseRequestBuilder extends BuilderImpl {
        private boolean throwAccessDenied = false;
        private boolean throwThrottleException = false;

        public BaseRequestBuilder throwAccessDenied(boolean denied) {
            throwAccessDenied = denied;
            return this;
        }

        public BaseRequestBuilder throwThrottleException(boolean val) {
            throwThrottleException = val;
            return this;
        }
    }
}
