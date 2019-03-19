package com.aws.cfn.resource;

import com.aws.cfn.LambdaWrapperTest;
import java.io.IOException;
import java.io.InputStream;

import com.aws.cfn.proxy.RequestContext;
import org.junit.Test;
import org.apache.commons.io.IOUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
public class SerializerTest {

    @Test
    public void deserialize_requestContext_withNestedCallbackContext() throws IOException {
        final InputStream requestStream = LambdaWrapperTest.loadRequestStream("request-context-with-callback.json");
        final String serializedRequest = IOUtils.toString(requestStream, "UTF-8");
        final Serializer serializer = new Serializer();

        final RequestContext requestContext = serializer.deserialize(serializedRequest, RequestContext.class);

        final Map<String, Object>  callbackContext = requestContext.getCallbackContext();
        final Map<String, String> nestedMap = (Map<String, String>) callbackContext.get("propertyObject");
        assertThat(nestedMap.get("nestedProperty"), is("testNestedValue"));
        assertThat(callbackContext.get("propertyTwo"),is("testValue"));
    }
}
