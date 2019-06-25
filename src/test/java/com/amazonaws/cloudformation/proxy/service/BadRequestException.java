package com.amazonaws.cloudformation.proxy.service;

import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

public class BadRequestException extends AwsServiceException {
    private static final long serialVersionUID = 1L;
    public BadRequestException(AwsServiceException.Builder builder) {
        super(builder);
    }
}
