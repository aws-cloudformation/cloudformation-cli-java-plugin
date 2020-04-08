package {{ package_name }};

import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class {{ operation }}Handler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<ServiceSdkClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        // TODO: Adjust Progress Chain according to your implementation

        return proxy.initiate("Service-Name::Read-Custom-Resource", proxyClient, model, callbackContext)
            .request(Translator::translateToReadRequest) // construct a body of a describe request
            .call(this::readResource) // make an api call
            .done(this::constructResourceModelFromResponse); // gather all properties of the resource
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest
     * @param proxyClient
     * @return
     */
    AwsResponse readResource(
        final AwsRequest awsRequest,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        AwsResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, ClientBuilder.getClient()::executeReadRequest);
        } catch (final ResourceConflictException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        }

        logger.log(String.format("%s [%s] has successfully been read.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return awsResponse;
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest
     * @param proxyClient
     * @return
     */
    AwsResponse constructResourceModelFromResponse(
        final AwsResponse awsResponse,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
        return progressEvent.success(Translator.translateFromReadRequest(awsResponse));
    }
}
