// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class BaseHandler<CallbackT> {

    public abstract ProgressEvent<{{ pojo_name }}, CallbackT> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<{{ pojo_name }}> request,
        final CallbackT callbackContext,
        final Logger logger);

}
