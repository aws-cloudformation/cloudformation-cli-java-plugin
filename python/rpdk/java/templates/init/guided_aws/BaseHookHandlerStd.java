package {{ package_name }};

import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;

// Placeholder for the functionality that could be shared across PreCreate/PreUpdate/PreDelete Handlers

public abstract class BaseHookHandlerStd extends BaseHookHandler<CallbackContext, TypeConfigurationModel> {
    @Override
    public ProgressEvent<HookTargetModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final HookHandlerRequest request,
            final CallbackContext callbackContext,
            final Logger logger,
            final TypeConfigurationModel typeConfiguration
    ) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger,
                typeConfiguration
        );
    }

    protected abstract ProgressEvent<HookTargetModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final HookHandlerRequest request,
            final CallbackContext callbackContext,
            final ProxyClient<SdkClient> proxyClient,
            final Logger logger,
            final TypeConfigurationModel typeConfiguration
    );
}
