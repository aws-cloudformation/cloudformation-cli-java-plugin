package {{ package_name }};

import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.var;
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
            .request(modifyResourceRequest) // construct a body of a request
            .call(executeModifyRequest) // make an api call
            .stabilize(commonStabilizer) // stabilize is describing the resource until it is in a certain status
            .progress()
            .then(postModificationUpdate) // post stabilization update
            .then(postModificationUpdate2); // post stabilization update2
    }

    // Sample lambda function to construct a modify request
    final Function<ResourceModel, Object> modifyResourceRequest = Translator::sampleModifyResourceRequest;

    // Inputs: {AwsRequest, SampleSdkClient} | Output: {AwsResponse}
    final BiFunction<Object, ProxyClient<SdkClient>, Object> executeModifyRequest =
        (
            final Object awsRequest, // AwsRequest
            final ProxyClient<SdkClient> proxyClient
        ) -> {
            // TODO: Implement client invocation of the request
            // hint: should return proxyClient.injectCredentialsAndInvokeV2(awsRequest, SampleSdkClient::execute);
            final var awsResponse = awsRequest;
            return awsResponse; // AwsResponse
        };

    // Sample lambda function to do subsequent operations after resource has been modified/stabilized
    final Function<ProgressEvent<ResourceModel, CallbackContext>,
        ProgressEvent<ResourceModel, CallbackContext>> postModificationUpdate = this::modifyResourceProperty;

    // Sample lambda function to do subsequent operations after resource has been modified/stabilized
    final Function<ProgressEvent<ResourceModel, CallbackContext>,
        ProgressEvent<ResourceModel, CallbackContext>> postModificationUpdate2 = this::modifyResourceOtherProperties;

    // put additional logic that is {{ operation }}Handler specific
}
