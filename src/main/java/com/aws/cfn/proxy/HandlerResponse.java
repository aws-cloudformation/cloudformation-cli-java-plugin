package com.aws.cfn.proxy;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This interface describes the response object for the provisioning request
 * @param <T> Type of resource model being provisioned
 */
@Data
@NoArgsConstructor
public class HandlerResponse<ResourceT> {
    private String bearerToken;
    private String errorCode;
    private String message;
    private String nextToken;
    private OperationStatus operationStatus;
    private ResponseData<ResourceT> responseData;
    private StabilizationData stabilizationData;
}
