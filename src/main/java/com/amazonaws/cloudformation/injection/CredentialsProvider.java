package com.amazonaws.cloudformation.injection;

import com.amazonaws.cloudformation.proxy.Credentials;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public interface CredentialsProvider {

    /**
     * Return the current set of credentials for initialising AWS SDK Clients
     */
    AwsSessionCredentials get();

    /**
     * Inject a new set of credentials (passed through from caller)
     */
    void setCredentials(final Credentials credentials);
}
