package {{ package_name }};

import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class {{ operation }}Handler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<SdkClient> proxyClient,
      final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // TODO: Adjust Progress Chain according to your implementation

        return proxy.initiate("Service-Name::{{ operation }}-Custom-Resource", proxyClient, model, callbackContext)
            .request(SAMPLE_REQUEST) // construct a body of a request
            .call(EXECUTE_SAMPLE_REQUEST) // make an api call
            .stabilize(STABILIZE_RESOURCE) // stabilize is describing the resource until it is in a certain status
            .progress()
            .then(SAMPLE_UPDATE); // post stabilization update
    }

    // put additional logic that is {{ operation }}Handler specific
}
