package {{ package_name }};

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

  /*
  * Methods and Functions that could be shared by Handlers should be in this class
  * Some placeholder for useful functions
  * */


  // waiting for a resource to be in a "ready" state
  final Callback<Object, Object, SdkClient, ResourceModel, CallbackContext, Boolean> commonStabilizer =
      (
          final Object awsRequest, // AwsRequest
          final Object awsResponse, // AwsResponse
          final ProxyClient<SdkClient> proxy,
          final ResourceModel model,
          final CallbackContext context
      ) -> isResourceStabilized();

  protected boolean isResourceStabilized() {
    // TODO: Stabilization logic - describing the resource until the in a "ready" state
    return true;
  }

  protected ProgressEvent<ResourceModel, CallbackContext> modifyResourceProperty(final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
    // TODO: Update logic - return progress event
    return ProgressEvent.progress(progressEvent.getResourceModel(), progressEvent.getCallbackContext());
  }

  protected ProgressEvent<ResourceModel, CallbackContext> modifyResourceOtherProperties(final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
    // TODO: Alternative Update logic if couldn't be implemented inside modifyResourceProperty - return progress event
    return ProgressEvent.success(progressEvent.getResourceModel(), progressEvent.getCallbackContext());
  }
}
