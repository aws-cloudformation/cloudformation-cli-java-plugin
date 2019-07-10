package {{ package_name }};

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public class {{ operation }}Handler extends SharedHandler {

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
