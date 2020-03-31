package {{ package_name }};

import java.util.function.Function;
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

        return proxy.initiate("Service-Name::{{operation}}-Custom-Resource", proxyClient, model, callbackContext)
            .request(DESCRIBE_REQUEST) // construct a body of a describe request
            .call(EXECUTE_SAMPLE_REQUEST) // make an api call
            .done(POST_EXECUTE); // gather all properties of the resource
    }

    // Sample lambda function to construct a describe request
    final Function<ResourceModel, Object> DESCRIBE_REQUEST = Translator::describeSampleResourceRequest;
    final Function<Object, ProgressEvent<ResourceModel, CallbackContext>> POST_EXECUTE =
        (
            final Object response // AwsResponse
        ) -> {
        // Construct resource model that contains all non-writeOnly properties
        final ResourceModel model = ResourceModel.builder()
            /*...*/
            .build();
        return ProgressEvent.defaultSuccessHandler(model);
    };

    // put additional logic that is {{operation}}Handler specific
}
