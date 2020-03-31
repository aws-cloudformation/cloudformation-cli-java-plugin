package {{ package_name }};

import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.var;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain.Callback;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final Logger logger) {
    return handleRequest(
        proxy,
        request,
        callbackContext != null ? callbackContext : new CallbackContext(),
        proxy.newProxy(ClientBuilder::getClient),
        logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<SdkClient> proxyClient,
      final Logger logger);

  // Methods and Functions that could be shared by Handlers should be in this class

  // Sample lambda function to construct a request
  final Function<ResourceModel, Object> SAMPLE_REQUEST = Translator::sampleResourceRequest;

  // Sample execution of the request
  // Inputs: AwsRequest, SampleSdkClient
  // Output: AwsResponse
  final BiFunction<Object, ProxyClient<SdkClient>, Object> EXECUTE_SAMPLE_REQUEST =
      (
          final Object awsRequest, // AwsRequest
          final ProxyClient<SdkClient> proxyClient
      ) -> {
        // TODO: Implement client invocation of the request
        // hint: should return proxyClient.injectCredentialsAndInvokeV2(awsRequest, SampleSdkClient::execute);
        final var awsResponse = awsRequest;
        return awsResponse; // AwsResponse
  };

  // waiting for a resource to be in a "ready" state
  final Callback<Object, Object, SdkClient, ResourceModel, CallbackContext, Boolean> STABILIZE_RESOURCE =
      (
          final Object awsRequest, // AwsRequest
          final Object awsResponse, // AwsResponse
          final ProxyClient<SdkClient> proxy,
          final ResourceModel model,
          final CallbackContext context
      ) -> isResourceStabilized();

  // Sample lambda function to do subsequent operations after resource has been created/stabilized
  final Function<ProgressEvent<ResourceModel, CallbackContext>, ProgressEvent<ResourceModel, CallbackContext>> SAMPLE_UPDATE =
      (final ProgressEvent<ResourceModel, CallbackContext> progressEvent)
          -> subsequentUpdateResource(progressEvent.getResourceModel(), progressEvent.getCallbackContext());


  protected boolean isResourceStabilized() {
    // TODO: Stabilization logic - describing the resource until the in a "ready" state
    return true;
  }

  protected ProgressEvent<ResourceModel, CallbackContext> subsequentUpdateResource(
      final ResourceModel model,
      final CallbackContext callbackContext) {
    // TODO: Subsequent Update logic - return progress event
    return ProgressEvent.success(model, callbackContext);
  }
}
