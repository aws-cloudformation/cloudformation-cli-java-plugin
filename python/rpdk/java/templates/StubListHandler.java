package {{ package_name }};

import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.Logger;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.OperationStatus;
import com.aws.cfn.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public class {{ operation }}Handler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<{{ pojo_name }}, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<{{ pojo_name }}> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final List<ResourceModel> models = new ArrayList<>();

        // TODO : put your code here

        return ProgressEvent.<{{ pojo_name }}, CallbackContext>builder()
            .resourceModels(models)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
