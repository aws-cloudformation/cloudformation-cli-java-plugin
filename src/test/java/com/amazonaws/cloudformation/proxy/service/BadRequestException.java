package com.amazonaws.cloudformation.proxy.service;

import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

public class BadRequestException extends AwsServiceException {
    private static final long serialVersionUID = 1L;
    private final SdkHttpResponse response;
    public BadRequestException(AwsServiceException.Builder builder) {
        super(builder);
        response = Mockito.mock(SdkHttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(400);
    }

    @Override
    public AwsErrorDetails awsErrorDetails() {
        return
            AwsErrorDetails.builder()
                .errorCode("BadRequest")
                .errorMessage("Bad Parameter in request")
                .sdkHttpResponse(response)
                .build();
    }
}
