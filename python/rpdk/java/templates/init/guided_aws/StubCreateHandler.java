package {{ package_name }};

import java.util.Objects;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class {{ operation }}Handler extends BaseHandlerStd {
    private static final String CREATED_RESOURCE_STATUS = "available";
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

        return ProgressEvent.progress(model, callbackContext)
            .then(progress -> checkForPreCreateResourceExistence(proxy, request, progress))
            .then(progress ->
                // If your service API throws 'ResourceAlreadyExistsException' for create requests then CreateHandler can return just proxy.initiate construction
                proxy.initiate("Service-Name::{{ operation }}-Custom-Resource", proxyClient, model, callbackContext)
                    .request(Translator::translateToCreateRequest) // construct a body of a request
                    .call(this::createResource) // make an api call
                    .stabilize(this::stabilizeOnCreate) // stabilize is describing the resource until it is in a certain status
                    .progress()
                    .then(progress -> postCreateUpdate(progress, proxyClient)); // post stabilization update
                )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * If your service API does not distinguish duplicate create requests against some identifier (e.g; resource Name)
     * and instead returns a 200 even though a resource already exists, you must first check if the resource exists here
     * NOTE: If your service API throws 'ResourceAlreadyExistsException' for create requests this method is not necessary
     * @param model
     */
    ProgressEvent<ResourceModel, CallbackContext> checkForPreCreateResourceExistence(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        try {
            new ReadHandler().handleRequest(proxy, request, callbackContext, logger);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        } catch (CfnNotFoundException e) {
            logger.log(model.getPrimaryIdentifier() + " does not exist; creating the resource.");
            return ProgressEvent.progress(model, callbackContext);
        }
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest
     * @param proxyClient
     * @return
     */
    AwsResponse createResource(
        final AwsRequest awsRequest,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        AwsResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, ClientBuilder.getClient()::executeCreateRequest);
        } catch (final CfnResourceConflictException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        }

        final String message = String.format("%s [%s] successfully created.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier());
        logger.log(message);

        return awsResponse;
    }

    /**
     * To account for eventual consistency, ensure you stabilize your create request so that a
     * subsequent Read will successfully describe the resource state that was provisioned
     */
    boolean stabilizeOnCreate(
        final ResourceModel model,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        final AwsRequest readRequest = Translator.translateToReadRequest(model);
        final AwsResponse readResponse = proxyClient.injectCredentialsAndInvokeV2(readRequest, ClientBuilder.getClient()::executeReadRequest);

        final status = readRequest.getResourceStatus();

        final boolean stabilized = status.equals(CREATED_RESOURCE_STATUS);

        if (stabalized) {
            logger.log(String.format("%s [%s] is in %s state. Stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), CREATED_RESOURCE_STATUS));
        } else {
            logger.log(String.format("%s [%s] is in %s state. Stabilization is in progress.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), status));
        }
        return stabilized;
    }

    /**
     * If your resource is provisioned through multiple API calls, you will need to apply each post-create update
     * step in a discrete call/stabilize chain to ensure the entire resource is provisioned as intended.
     * @param progressEvent
     * @return
     */
    ProgressEvent<ResourceModel, CallbackContext> postCreateUpdate(
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
        final ProxyClient<ServiceSdkClient> proxyClient) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToUpdateRequest(model), ClientBuilder.getClient()::executeUpdateResource);
        } catch (final CfnResourceConflictException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        }

        final String message = String.format("%s [%s] successfully updated.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier());
        logger.log(message);
    }
}
