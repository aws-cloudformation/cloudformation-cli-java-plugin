package com.aws.cfn.injection;

import com.aws.cfn.proxy.Credentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class PlatformCredentialsProvider {

    private AwsSessionCredentials awsSessionCredentials;

    public AwsSessionCredentials get() {
        return this.awsSessionCredentials;
    }

    public void setCredentials(final Credentials credentials) {
        this.awsSessionCredentials = AwsSessionCredentials.create(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
    }
}
