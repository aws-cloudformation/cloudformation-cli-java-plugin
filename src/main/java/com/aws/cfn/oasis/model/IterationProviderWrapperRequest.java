package com.aws.cfn.oasis.model;

import com.aws.cfn.proxy.Credentials;
import com.aws.cfn.proxy.RequestContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;

/**
 * This represents the request to the Lambda container from the
 * CloudFormation workflows to the handler for generating template iterations
 */
@Value
@EqualsAndHashCode
@AllArgsConstructor
@Builder(toBuilder = true)
public class IterationProviderWrapperRequest<InputT, CallbackT> {
    @NonNull private final Credentials credentials;
    @NonNull private final OperationInfo operationInfo;
    @NonNull private final String bearerToken;
    @Nullable private final InputT inputConfiguration;
    @NonNull private final Templates templates;
    @NonNull private RequestContext<CallbackT> requestContext;
}
