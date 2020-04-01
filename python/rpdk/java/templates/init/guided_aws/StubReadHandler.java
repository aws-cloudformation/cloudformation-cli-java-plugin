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

        return proxy.initiate("Service-Name::{{operation}}-Custom-Resource", proxyClient, model, callbackContext)
            .request(describeResourceRequest) // construct a body of a describe request
            .call(executeDescribeRequest) // make an api call
            .done(postExecution); // gather all properties of the resource
    }

    // Sample lambda function to construct a describe request
    final Function<ResourceModel, Object> describeResourceRequest = Translator::sampleDescribeResourceRequest;

    // Inputs: {AwsRequest, SampleSdkClient} | Output: {AwsResponse}
    final BiFunction<Object, ProxyClient<SdkClient>, Object> executeDescribeRequest =
        (
            final Object awsRequest, // AwsRequest
            final ProxyClient<SdkClient> proxyClient
        ) -> {
            // TODO: Implement client invocation of the request
            // hint: should return proxyClient.injectCredentialsAndInvokeV2(awsRequest, SampleSdkClient::execute);
            final var awsResponse = awsRequest;
            return awsResponse; // AwsResponse
        };

    final Function<Object, ProgressEvent<ResourceModel, CallbackContext>> postExecution =
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
