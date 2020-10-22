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
package software.amazon.cloudformation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.TestContext;
import software.amazon.cloudformation.TestModel;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.RequestData;

public class SerializerTest {

    private static final String TEST_DATA_BASE_PATH = "src/test/java/software/amazon/cloudformation/data/%s";

    private final TypeReference<
        HandlerRequest<TestModel, TestContext>> typeReference = new TypeReference<HandlerRequest<TestModel, TestContext>>() {
        };

    public static String loadRequestJson(final String fileName) throws IOException {
        final File file = new File(String.format(TEST_DATA_BASE_PATH, fileName));

        try {
            final InputStream inputStream = new FileInputStream(file);
            return IOUtils.toString(inputStream, "UTF-8");
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        final Serializer serializer = new Serializer();
        serializer.serialize(new TestModel());
    }

    @Test
    public void testDeserialize_AccuratePayload() throws IOException {
        final Serializer s = new Serializer();

        final String in = loadRequestJson("create.request.json");

        final HandlerRequest<TestModel, TestContext> r = s.deserialize(in, typeReference);

        assertThat(r).isNotNull();
        assertThat(r.getAction()).isEqualTo(Action.CREATE);
        assertThat(r.getAwsAccountId()).isEqualTo("123456789012");
        assertThat(r.getBearerToken()).isEqualTo("123456");
        assertThat(r.getRegion()).isEqualTo("us-east-1");
        assertThat(r.getBearerToken()).isEqualTo("123456");
        assertThat(r.getRequestData()).isNotNull();
        assertThat(r.getResourceType()).isEqualTo("AWS::Test::TestModel");
        assertThat(r.getResourceTypeVersion()).isEqualTo("1.0");
        assertThat(r.getStackId())
            .isEqualTo("arn:aws:cloudformation:us-east-1:123456789012:stack/SampleStack/e722ae60-fe62-11e8-9a0e-0ae8cc519968");
        assertThat(r.getCallbackContext()).isNull();

        final RequestData<TestModel> requestData = r.getRequestData();
        assertThat(requestData.getCallerCredentials()).isNotNull();
        assertThat(requestData.getCallerCredentials().getAccessKeyId()).isEqualTo("IASAYK835GAIFHAHEI23");
        assertThat(requestData.getCallerCredentials().getSecretAccessKey()).isEqualTo("66iOGPN5LnpZorcLr8Kh25u8AbjHVllv5/poh2O0");
        assertThat(requestData.getCallerCredentials().getSessionToken());
        assertThat(requestData.getLogicalResourceId()).isEqualTo("myBucket");
        assertThat(requestData.getStackTags()).containsExactly(entry("tag1", "abc"));
        assertThat(requestData.getPreviousResourceProperties()).isNull();
        assertThat(requestData.getResourceProperties()).isNotNull();
        assertThat(requestData.getResourceProperties().getProperty1()).isEqualTo("abc");
        assertThat(requestData.getResourceProperties().getProperty2()).isEqualTo(123);
    }

    @Test
    public void testDeserialize_ExtranousRequestFields_AreIncluded() throws IOException {
        final Serializer s = new Serializer();

        // serialization is intentionally loose to minimise disruption on interface
        // changes,
        // however but model validation will consider raw payload
        final String in = loadRequestJson("create.request.with-extraneous-request-fields.json");

        final HandlerRequest<TestModel, TestContext> r = s.deserialize(in, typeReference);

        assertThat(r).isNotNull();
        assertThat(r.getAction()).isEqualTo(Action.CREATE);
        assertThat(r.getAwsAccountId()).isEqualTo("123456789012");
        assertThat(r.getBearerToken()).isEqualTo("123456");
        assertThat(r.getRegion()).isEqualTo("us-east-1");
        assertThat(r.getRequestContext()).isNotNull();
        assertThat(r.getRequestData()).isNotNull();
        assertThat(r.getResourceType()).isEqualTo("AWS::Test::TestModel");
        assertThat(r.getResourceTypeVersion()).isEqualTo("1.0");
        assertThat(r.getStackId())
            .isEqualTo("arn:aws:cloudformation:us-east-1:123456789012:stack/SampleStack/e722ae60-fe62-11e8-9a0e-0ae8cc519968");
        assertThat(r.getCallbackContext()).isNull();

        final RequestData<TestModel> requestData = r.getRequestData();
        assertThat(requestData.getCallerCredentials()).isNotNull();
        assertThat(requestData.getCallerCredentials().getAccessKeyId()).isEqualTo("IASAYK835GAIFHAHEI23");
        assertThat(requestData.getCallerCredentials().getSecretAccessKey()).isEqualTo("66iOGPN5LnpZorcLr8Kh25u8AbjHVllv5/poh2O0");
        assertThat(requestData.getCallerCredentials().getSessionToken())
            .isEqualTo("lameHS2vQOknSHWhdFYTxm2eJc1JMn9YBNI4nV4mXue945KPL6DHfW8EsUQT5zwssYEC1NvYP9yD6Y5s5lKR3chflOHPFsIe6eqg");
        assertThat(requestData.getLogicalResourceId()).isEqualTo("myBucket");
        assertThat(requestData.getStackTags()).containsExactly(entry("tag1", "abc"));

        assertThat(requestData.getPreviousResourceProperties()).isNull();
        assertThat(requestData.getResourceProperties()).isNotNull();
        assertThat(requestData.getResourceProperties().getProperty1()).isEqualTo("abc");
        assertThat(requestData.getResourceProperties().getProperty2()).isEqualTo(123);
    }

    @Test
    public void testDeserialize_ExtranousModelFields_AreAllowed() throws IOException {
        final Serializer s = new Serializer();

        // serialization is intentionally loose to minimise disruption on interface
        // changes,
        // however but model validation will consider raw payload
        final String in = loadRequestJson("create.request.with-extraneous-model-fields.json");

        final HandlerRequest<TestModel, TestContext> r = s.deserialize(in, typeReference);

        assertThat(r).isNotNull();
        assertThat(r.getAction()).isEqualTo(Action.CREATE);
        assertThat(r.getAwsAccountId()).isEqualTo("123456789012");
        assertThat(r.getBearerToken()).isEqualTo("123456");
        assertThat(r.getRegion()).isEqualTo("us-east-1");
        assertThat(r.getRequestContext()).isNotNull();
        assertThat(r.getCallbackContext()).isNull();
        assertThat(r.getRequestData()).isNotNull();
        assertThat(r.getResourceType()).isEqualTo("AWS::Test::TestModel");
        assertThat(r.getResourceTypeVersion()).isEqualTo("1.0");
        assertThat(r.getStackId())
            .isEqualTo("arn:aws:cloudformation:us-east-1:123456789012:stack/SampleStack/e722ae60-fe62-11e8-9a0e-0ae8cc519968");

        final RequestData<TestModel> requestData = r.getRequestData();
        assertThat(requestData.getCallerCredentials()).isNotNull();
        assertThat(requestData.getCallerCredentials().getAccessKeyId()).isEqualTo("IASAYK835GAIFHAHEI23");
        assertThat(requestData.getCallerCredentials().getSecretAccessKey()).isEqualTo("66iOGPN5LnpZorcLr8Kh25u8AbjHVllv5/poh2O0");
        assertThat(requestData.getCallerCredentials().getSessionToken())
            .isEqualTo("lameHS2vQOknSHWhdFYTxm2eJc1JMn9YBNI4nV4mXue945KPL6DHfW8EsUQT5zwssYEC1NvYP9yD6Y5s5lKR3chflOHPFsIe6eqg");
        assertThat(requestData.getLogicalResourceId()).isEqualTo("myBucket");
        assertThat(requestData.getStackTags()).containsExactly(entry("tag1", "abc"));

        assertThat(requestData.getPreviousResourceProperties()).isNull();
        assertThat(requestData.getResourceProperties()).isNotNull();
        assertThat(requestData.getResourceProperties().getProperty1()).isEqualTo("abc");
        assertThat(requestData.getResourceProperties().getProperty2()).isEqualTo(123);
    }
}
