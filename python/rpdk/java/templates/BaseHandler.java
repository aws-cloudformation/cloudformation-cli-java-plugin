// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

public abstract class BaseHandler<CallbackT> {

    public abstract ProgressEvent<{{ pojo_name }}, CallbackT> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<{{ pojo_name }}> request,
        final CallbackT callbackContext,
        final Logger logger);

}
