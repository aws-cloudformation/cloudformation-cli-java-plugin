package com.amazonaws.cloudformation.proxy.service;

import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

public class AccessDenied extends AwsServiceException {
    private static final long serialVersionUID = 1L;
    private final SdkHttpResponse response;
    public AccessDenied(AwsServiceException.Builder builder) {
        super(builder);
        response = Mockito.mock(SdkHttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(401);
    }

    @Override
    public AwsErrorDetails awsErrorDetails() {
        return
            AwsErrorDetails.builder()
                .errorCode("AccessDenied: 401")
                .errorMessage("Token invalid")
                .sdkHttpResponse(response)
                .build();
    }
}
