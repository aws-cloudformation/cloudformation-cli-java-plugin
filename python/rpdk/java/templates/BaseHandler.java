// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.Logger;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.ResourceHandlerRequest;

public abstract class BaseHandler<CallbackT> {

    public abstract ProgressEvent<{{ pojo_name }}, CallbackT> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<{{ pojo_name }}> request,
        final CallbackT callbackContext,
        final Logger logger);

}
