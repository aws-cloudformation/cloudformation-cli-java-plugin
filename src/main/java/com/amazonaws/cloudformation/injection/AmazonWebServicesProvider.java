package com.amazonaws.cloudformation.injection;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AmazonWebServicesProvider {

    final protected List<CredentialsProvider> credentialsProviderList;

    protected AmazonWebServicesProvider(final CredentialsProvider... credentialsProviders) {
        this.credentialsProviderList = Arrays.asList(credentialsProviders);
    }

    protected List<AwsCredentialsProvider> getCredentialsProviders() {
        return credentialsProviderList.stream()
                // Filter out credential providers which have null credentials assigned
                .filter(credentialsProvider -> credentialsProvider.get() != null)
                .map(credentialsProvider -> StaticCredentialsProvider.create(credentialsProvider.get()))
                .collect(Collectors.toList());
    }

    protected AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(this.credentialsProviderList.get(0).get());
    }
}
