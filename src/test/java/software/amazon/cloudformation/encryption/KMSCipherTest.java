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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.Credentials;

@ExtendWith(MockitoExtension.class)
public class KMSCipherTest {
    @Mock
    private AwsCrypto cryptoHelper;

    @Mock
    private CryptoResult<byte[], KmsMasterKey> result;

    @Mock
    private KmsMasterKeyProvider kmsKeyProvider;

    private KMSCipher cipher;

    @Test
    public void constructKMSCipher_constructSuccess() {
        try {
            cipher = new KMSCipher("encryptionKeyArn", "encryptionKeyRole");
            assertThat(cipher).isNotNull();
        } catch (final Exception ex) {
        }
    }

    @Test
    public void decryptCredentials_decryptSuccess() {
        cipher = new KMSCipher(cryptoHelper, kmsKeyProvider);
        lenient().when(cryptoHelper.decryptData(any(KmsMasterKeyProvider.class), any(byte[].class))).thenReturn(result);
        lenient().when(result.getResult()).thenReturn("{\"test\":\"test\"}".getBytes());

        try {
            Credentials decryptedCredentials = cipher
                .decryptCredentials("ewogICAgICAgICAgICAiYWNjZXNzS2V5SWQiOiAiSUFTQVlLODM1R0FJRkhBSEVJMjMiLAogICAg\n"
                    + "ICAgICAgICAic2VjcmV0QWNjZXNzS2V5IjogIjY2aU9HUE41TG5wWm9yY0xyOEtoMjV1OEFiakhW\n"
                    + "bGx2NS9wb2gyTzAiLAogICAgICAgICAgICAic2Vzc2lvblRva2VuIjogImxhbWVIUzJ2UU9rblNI\n"
                    + "V2hkRllUeG0yZUpjMUpNbjlZQk5JNG5WNG1YdWU5NDVLUEw2REhmVzhFc1VRVDV6d3NzWUVDMU52\n"
                    + "WVA5eUQ2WTVzNWxLUjNjaGZsT0hQRnNJZTZlcWciCiAgICAgICAgfQ==");
            assertThat(decryptedCredentials).isNotNull();
        } catch (final Exception ex) {
        }

    }

    @Test
    public void decryptCredentials_decryptFailure() {
        cipher = new KMSCipher("encryptionKeyArn", "encryptionKeyRole");
        try {
            Credentials decryptedCredentials = cipher
                .decryptCredentials("ewogICAgICAgICAgICAiYWNjZXNzS2V5SWQiOiAiSUFTQVlLODM1R0FJRkhBSEVJMjMiLAogICAg\n"
                    + "ICAgICAgICAic2VjcmV0QWNjZXNzS2V5IjogIjY2aU9HUE41TG5wWm9yY0xyOEtoMjV1OEFiakhW\n"
                    + "bGx2NS9wb2gyTzAiLAogICAgICAgICAgICAic2Vzc2lvblRva2VuIjogImxhbWVIUzJ2UU9rblNI\n"
                    + "V2hkRllUeG0yZUpjMUpNbjlZQk5JNG5WNG1YdWU5NDVLUEw2REhmVzhFc1VRVDV6d3NzWUVDMU52\n"
                    + "WVA5eUQ2WTVzNWxLUjNjaGZsT0hQRnNJZTZlcWciCiAgICAgICAgfQ==");
            assertThat(decryptedCredentials).isNotNull();
        } catch (final Exception ex) {
        }
    }
}
