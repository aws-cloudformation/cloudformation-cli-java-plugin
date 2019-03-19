package {{ package_name }};

import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.Logger;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.OperationStatus;
import com.aws.cfn.proxy.ResourceHandlerRequest;

import java.util.Map;

public class {{ operation }}Handler extends BaseHandler {

    @Override
    public ProgressEvent handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<{{ pojo_name }}> request,
        final Map<String, Object> callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // TODO : put your code here

        final ProgressEvent pe = new ProgressEvent();
        pe.setResourceModel(model);
        pe.setStatus(OperationStatus.SUCCESS);
        return pe;
    }
}
