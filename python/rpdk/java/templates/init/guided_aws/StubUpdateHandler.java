package {{ package_name }};

import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class {{ operation }}Handler extends BaseHandlerStd {
    private static final String UPDATED_RESOURCE_STATUS = "updated";
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

        return proxy.initiate("Service-Name::Update-Custom-Resource", proxyClient, model, callbackContext)
            .request(Translator::translateToUpdateRequest) // construct a body of a request
            .call(this::updateResource) // make an api call
            .stabilize(this::stabilizeOnUpdate) // stabilize is describing the resource until it is in a certain status
            .progress()
            .then(progress -> postUpdate(progress, proxyClient)) // post stabilization update
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest
     * @param proxyClient
     * @return
     */
    AwsResponse updateResource(
        final AwsRequest awsRequest,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        AwsResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, ClientBuilder.getClient()::executeUpdateRequest);
        } catch (final ResourceConflictException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        }

        logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return awsResponse;
    }

    /**
     * To account for eventual consistency, ensure you stabilize your update request so that a
     * subsequent Read will successfully describe the resource state that was provisioned
     */
    Boolean stabilizeOnUpdate(
        final ResourceModel model,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        final AwsRequest readRequest = Translator.translateToReadRequest(model);
        final AwsResponse readResponse = proxyClient.injectCredentialsAndInvokeV2(readRequest, ClientBuilder.getClient()::executeReadRequest);

        final status = readRequest.getResourceStatus();

        final boolean stabilized = status.equals(UPDATED_RESOURCE_STATUS);

        if (stabalized) {
            logger.log(String.format("%s [%s] is in %s state. Stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), UPDATED_RESOURCE_STATUS));
        } else {
            logger.log(String.format("%s [%s] is in %s state. Stabilization is in progress.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), status));
        }
        return stabilized;
    }

    /**
     * If your resource is provisioned through multiple API calls, you will need to apply each subsequent update
     * step in a discrete call/stabilize chain to ensure the entire resource is provisioned as intended.
     * @param progressEvent
     * @return
     */
    ProgressEvent<ResourceModel, CallbackContext> postUpdate(
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToUpdateRequest(model), ClientBuilder.getClient()::executeUpdateResource);
        } catch (final ResourceConflictException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        }

        logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return progressEvent;
    }
}
