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
package software.amazon.cloudformation.proxy.handler.hook;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.StdCallbackContext;
import software.amazon.cloudformation.proxy.handler.Model;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.service.ServiceClient;

public class PreCreateHandler {

    private final ServiceClient client;

    public PreCreateHandler(ServiceClient client) {
        this.client = client;
    }

    public ProgressEvent<Model, StdCallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                  final HookHandlerRequest request,
                                                                  final StdCallbackContext context,
                                                                  final LoggerProxy loggerProxy) {

        final StdCallbackContext cxt = context == null ? new StdCallbackContext() : context;
        return ProgressEvent.success(null, cxt, "Hook invocation completed.");
    }
}
