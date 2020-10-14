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
package software.amazon.cloudformation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.injection.CredentialsProvider;
import software.amazon.cloudformation.loggers.CloudWatchLogPublisher;
import software.amazon.cloudformation.loggers.JavaLogPublisher;
import software.amazon.cloudformation.loggers.LogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;

public abstract class ExecutableWrapper<ResourceT, CallbackT> extends AbstractWrapper<ResourceT, CallbackT> {
    private Logger platformLogger = LoggerFactory.getLogger("GLOBAL");

    public ExecutableWrapper() {
        super();
    }

    /*
     * This .ctor provided for testing
     */
    public ExecutableWrapper(final CredentialsProvider providerCredentialsProvider,
                             final LogPublisher platformEventsLogger,
                             final CloudWatchLogPublisher providerEventsLogger,
                             final MetricsPublisher providerMetricsPublisher,
                             final SchemaValidator validator,
                             final Serializer serializer,
                             final SdkHttpClient httpClient) {
        super(providerCredentialsProvider, platformEventsLogger, providerEventsLogger, providerMetricsPublisher, validator,
              serializer, httpClient);
    }

    public void handleRequest(final InputStream inputStream, final OutputStream outputStream) throws IOException,
        TerminalException {

        if (platformLogPublisher == null) {
            platformLogPublisher = new JavaLogPublisher(platformLogger);
        }
        this.platformLoggerProxy.addLogPublisher(platformLogPublisher);
        processRequest(inputStream, outputStream);
        outputStream.flush();
    }
}
