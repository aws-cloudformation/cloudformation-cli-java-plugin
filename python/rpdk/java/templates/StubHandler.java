package {{ package_name }};

import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.Logger;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.OperationStatus;
import com.aws.cfn.proxy.ResourceHandlerRequest;

public class {{ operation }}Handler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<{{ pojo_name }}, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<{{ pojo_name }}> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // TODO : put your code here

        return ProgressEvent.builder()
            .resourceModel(model)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
