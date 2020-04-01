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
            .request(deleteResourceRequest) // construct a body of a request
            .call(executeDeleteRequest) // make an api call
            .stabilize(commonStabilizer) // stabilize is describing the resource until it is in a certain status
            .success(); // success if stabilized
    }

    // Sample lambda function to construct a delete request
    final Function<ResourceModel, Object> deleteResourceRequest = Translator::sampleDeleteResourceRequest;

    // Inputs: {AwsRequest, SampleSdkClient} | Output: {AwsResponse}
    final BiFunction<Object, ProxyClient<SdkClient>, Object> executeDeleteRequest =
        (
            final Object awsRequest, // AwsRequest
            final ProxyClient<SdkClient> proxyClient
        ) -> {
            // TODO: Implement client invocation of the request
            // hint: should return proxyClient.injectCredentialsAndInvokeV2(awsRequest, SampleSdkClient::execute);
            final var awsResponse = awsRequest;
            return awsResponse; // AwsResponse
        };

    // put additional logic that is {{ operation }}Handler specific
}
