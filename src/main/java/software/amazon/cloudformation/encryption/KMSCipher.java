/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.encryption;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.PredefinedClientConfigurations;
import com.amazonaws.auth.AWSSessionCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.exception.AwsCryptoException;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bouncycastle.util.encoders.Base64;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.cloudformation.exceptions.EncryptionException;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.resource.Serializer;

public class KMSCipher implements Cipher {
    private static final int STS_CONNECTION_TIMEOUT_MILLIS = 10000;
    private static final int STS_CONNECTION_TTL_MILLIS = 60000;
    private static final int STS_CLIENT_EXECUTION_TIMEOUT_MILLIS = 10000;
    private static final int STS_REQUEST_TIMEOUT_MILLIS = 10000;
    private static final int STS_SOCKET_TIMEOUT_MILLIS = 10000;
    private static final int STS_MAX_ERROR_RETRY = 3;

    private final AwsCrypto cryptoHelper;
    private final MasterKeyProvider<KmsMasterKey> kmsKeyProvider;
    private final Serializer serializer;
    private final TypeReference<Credentials> credentialsTypeReference;

    public KMSCipher(final String encryptionKeyArn,
                     final String encryptionKeyRole) {
        final String region = SdkSystemSetting.AWS_REGION.getStringValue().map(Object::toString).orElse("us-east-1");

        final ClientConfiguration clientConfiguration = PredefinedClientConfigurations.defaultConfig()
            .withConnectionTimeout(STS_CONNECTION_TIMEOUT_MILLIS).withConnectionTTL(STS_CONNECTION_TTL_MILLIS)
            .withClientExecutionTimeout(STS_CLIENT_EXECUTION_TIMEOUT_MILLIS).withRequestTimeout(STS_REQUEST_TIMEOUT_MILLIS)
            .withSocketTimeout(STS_SOCKET_TIMEOUT_MILLIS).withMaxErrorRetry(STS_MAX_ERROR_RETRY);

        final AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
            .withClientConfiguration(clientConfiguration).withRegion(region).build();

        final AWSSessionCredentialsProvider assumeRoleSessionCredentialsProvider = getAssumeRoleSessionCredentialProvider(
            encryptionKeyRole, stsClient);

        this.kmsKeyProvider = KmsMasterKeyProvider.builder().withCredentials(assumeRoleSessionCredentialsProvider)
            .withDefaultRegion(region).buildStrict(encryptionKeyArn);

        this.cryptoHelper = AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.ForbidEncryptAllowDecrypt).build();
        this.serializer = new Serializer();
        this.credentialsTypeReference = getCredentialsTypeReference();
    }

    // constructor for unit testing
    public KMSCipher(final AwsCrypto cryptoHelper,
                     final MasterKeyProvider<KmsMasterKey> kmsKeyProvider) {
        this.kmsKeyProvider = kmsKeyProvider;
        this.cryptoHelper = cryptoHelper;
        this.serializer = new Serializer();
        this.credentialsTypeReference = getCredentialsTypeReference();
    }

    @Override
    public Credentials decryptCredentials(final String encryptedCredentials) {
        try {
            final CryptoResult<byte[],
                KmsMasterKey> result = cryptoHelper.decryptData(kmsKeyProvider, Base64.decode(encryptedCredentials));
            final Credentials credentials = serializer.deserialize(new String(result.getResult(), StandardCharsets.UTF_8),
                this.credentialsTypeReference);
            if (credentials == null) {
                throw new EncryptionException("Failed to decrypt credentials. Decrypted credentials are 'null'.");
            }

            return credentials;
        } catch (final IOException | AwsCryptoException e) {
            throw new EncryptionException("Failed to decrypt credentials.", e);
        }
    }

    private static TypeReference<Credentials> getCredentialsTypeReference() {
        return new TypeReference<Credentials>() {
        };
    }

    private STSAssumeRoleSessionCredentialsProvider
        getAssumeRoleSessionCredentialProvider(final String encryptionKeyRole, final AWSSecurityTokenService stsClient) {
        return new STSAssumeRoleSessionCredentialsProvider.Builder(encryptionKeyRole, UUID.randomUUID().toString())
            .withStsClient(stsClient).build();
    }
}
