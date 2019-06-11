package com.amazonaws.cloudformation.proxy.handler;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ProxyClient;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.proxy.StdCallbackContext;
import com.amazonaws.cloudformation.proxy.service.CreateRequest;
import com.amazonaws.cloudformation.proxy.service.ServiceClient;

public class CreateHandler {

    private final ServiceClient client;
    public CreateHandler(ServiceClient client) {
        this.client = client;
    }


    public ProgressEvent<Model, StdCallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<Model> request,
        final StdCallbackContext context,
        final Logger logger) {

        final Model model = request.getDesiredResourceState();

        ProxyClient<ServiceClient> client = proxy.newProxy(() -> this.client);
        return proxy.initiate("client:createRepository", client, model, context)
                .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .done((request1, response, client1, model1, context1) ->
                    new ReadHandler(this.client).handleRequest(proxy, request, context, logger)
                );
    }

}
