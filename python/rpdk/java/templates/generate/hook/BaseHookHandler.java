// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;

public abstract class BaseHookHandler<CallbackT, ConfigurationT> {

    public abstract ProgressEvent<HookTargetModel, CallbackT> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final HookHandlerRequest request,
        final CallbackT callbackContext,
        final Logger logger,
        final ConfigurationT typeConfiguration);

}
