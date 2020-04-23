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
package software.amazon.cloudformation.proxy.handler;

import java.time.Duration;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.StdCallbackContext;
import software.amazon.cloudformation.proxy.delay.Constant;
import software.amazon.cloudformation.proxy.service.DescribeRequest;
import software.amazon.cloudformation.proxy.service.ServiceClient;

public class ReadHandler {

    private final ServiceClient client;

    ReadHandler(ServiceClient client) {
        this.client = client;
    }

    public ProgressEvent<Model, StdCallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                  final ResourceHandlerRequest<Model> request,
                                                                  final StdCallbackContext context,
                                                                  final LoggerProxy loggerProxy) {

        final Model model = request.getDesiredResourceState();
        final StdCallbackContext cxt = context == null ? new StdCallbackContext() : context;
        ProxyClient<ServiceClient> client = proxy.newProxy(() -> this.client);
        return proxy.initiate("client:readRepository", client, model, cxt).translateToServiceRequest(m -> {
            DescribeRequest.Builder builder = new DescribeRequest.Builder();
            builder.repoName(m.getRepoName());
            return builder.build();
        }).backoffDelay(Constant.of().delay(Duration.ofSeconds(3)).timeout(Duration.ofSeconds(9)).build())
            .makeServiceCall((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::describeRepository)).done(r -> {
                model.setRepoName(r.getRepoName());
                model.setArn(r.getRepoArn());
                model.setCreated(r.getCreatedWhen());
                return ProgressEvent.success(model, context);
            });
    }

}
