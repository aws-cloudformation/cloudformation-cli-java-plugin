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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.loggers.LambdaLogPublisher;

public abstract class LambdaWrapper<ResourceT, CallbackT> extends Wrapper<ResourceT, CallbackT> implements RequestStreamHandler {
    @Override
    public void handleRequest(final InputStream inputStream, final OutputStream outputStream, final Context context)
        throws IOException,
        TerminalException {
        if (platformLogPublisher == null) {
            platformLogPublisher = new LambdaLogPublisher(context.getLogger());
        }
        this.platformLoggerProxy.addLogPublisher(platformLogPublisher);
        processRequest(inputStream, outputStream);
    }
}
