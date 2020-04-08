package {{ package_name }};

import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
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

        return proxy.initiate("Service-Name::Delete-Custom-Resource", proxyClient, model, callbackContext)
            .request(Translator::translateToDeleteRequest) // construct a body of a request
            .call(this::deleteResource) // make an api call
            .stabilize(this::stabilizeOnDelete) // stabilize is describing the resource until it is in a certain status
            .success(); // success if stabilized
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest
     * @param proxyClient
     * @return
     */
    AwsResponse deleteResource(
        final AwsRequest awsRequest,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        AwsResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, ClientBuilder.getClient()::executeDeleteRequest);
        } catch (final ResourceIsInInvalidState e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        }

        logger.log(String.format("%s [%s] successfully deleted.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return awsResponse;
    }

    /**
     * It is important to make sure that your resource has been deleted
     * in order not to generate a false positive stack event
     */
    Boolean stabilizeOnDelete(
        final ResourceModel model,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        final AwsRequest readRequest = Translator.translateToReadRequest(model);
        try {
            proxy.injectCredentialsAndInvokeV2(readRequest, ClientBuilder.getClient()::executeReadRequest);
            logger.log(String.format("%s [%s] is not yet deleted. Stabilization is in progress.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
            return false; // not stabilized
        } catch(final ResourceNotFoundException e) {
            logger.log(String.format("%s [%s] has successfully been deleted. Stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
            return true; // stabilized
        }
    }
}
