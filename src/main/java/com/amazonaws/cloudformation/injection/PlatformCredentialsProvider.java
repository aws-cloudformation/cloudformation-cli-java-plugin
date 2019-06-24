package com.amazonaws.cloudformation.injection;

import com.amazonaws.cloudformation.proxy.Credentials;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class PlatformCredentialsProvider implements CredentialsProvider {

    private AwsSessionCredentials awsSessionCredentials;

    public AwsSessionCredentials get() {
        return this.awsSessionCredentials;
    }

    public void setCredentials(final Credentials credentials) {
        this.awsSessionCredentials = AwsSessionCredentials.create(credentials.getAccessKeyId(), credentials.getSecretAccessKey(),
            credentials.getSessionToken());
    }
}
