package com.amazonaws.cloudformation.proxy;

import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

public class ThrottleException extends AwsServiceException {
    private static final long serialVersionUID = 1L;
    private final SdkHttpResponse response;
    public ThrottleException(Builder builder) {
        super(builder);
        response = Mockito.mock(SdkHttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(429);
    }

    @Override
    public AwsErrorDetails awsErrorDetails() {
        return AwsErrorDetails.builder()
            .errorCode("ThrottleException")
            .errorMessage("Temporary Limit Exceeded")
            .sdkHttpResponse(response)
            .build();

    }
}
