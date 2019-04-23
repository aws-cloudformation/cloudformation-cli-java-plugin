package com.aws.cfn.oasis.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import javax.annotation.Nullable;

/**
 * The model representing the request to the handler itself
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class IterationProviderHandlerRequest<InputT> {
    @NonNull private final OperationInfo operationInfo;
    @NonNull private final String clientRequestToken;
    @Nullable private final InputT configuration;
    @NonNull private final Templates templates;
}
