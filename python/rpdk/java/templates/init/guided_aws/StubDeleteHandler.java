package {{ package_name }};

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SdkClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        // TODO: Adjust Progress Chain according to your implementation
        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java

        return ProgressEvent.progress(model, callbackContext)

            // STEP 1 [check if resource already exists]
            // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            // if target API does not support 'ResourceNotFoundException' then following check is required
            .then(progress -> checkForPreDeleteResourceExistence(proxy, request, progress))

            // STEP 2.0 [delete/stabilize progress chain - required for resource deletion]
            .then(progress ->
                // If your service API throws 'ResourceNotFoundException' for delete requests then DeleteHandler can return just proxy.initiate construction
                // STEP 2.0 [initialize a proxy context]
                proxy.initiate("{{ call_graph }}::{{ operation }}", proxyClient, model, callbackContext)

                    // STEP 2.1 [TODO: construct a body of a request]
                    .translateToServiceRequest(Translator::translateToDeleteRequest)

                    // STEP 2.2 [TODO: make an api call]
                    .makeServiceCall(this::deleteResource)

                    // STEP 2.3 [TODO: stabilize step is not necessarily required but typically involves describing the resource until it is in a certain status, though it can take many forms]
                    // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                    .stabilize(this::stabilizedOnDelete)
                    .success());
    }

    /**
     * If your service API does not return ResourceNotFoundException on delete requests against some identifier (e.g; resource Name)
     * and instead returns a 200 even though a resource already deleted, you must first check if the resource exists here
     * NOTE: If your service API throws 'ResourceNotFoundException' for delete requests this method is not necessary
     * @param proxy Amazon webservice proxy to inject credentials correctly.
     * @param request incoming resource handler request
     * @param progressEvent event of the previous state indicating success, in progress with delay callback or failed state
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> checkForPreDeleteResourceExistence(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        try {
            new ReadHandler().handleRequest(proxy, request, callbackContext, logger);
            return ProgressEvent.progress(model, callbackContext);
        } catch (CfnNotFoundException e) { // ResourceNotFoundException
            logger.log(String.format("%s does not exist. RequestId: %s. Message: %s",
                model.getPrimaryIdentifier(),
                request.getClientRequestToken(),
                e.getMessage()));
            throw e;
        }
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private AwsResponse deleteResource(
        final AwsRequest awsRequest,
        final ProxyClient<SdkClient> proxyClient) {
        AwsResponse awsResponse = null;
        try {

            // TODO: put your delete resource code here

        } catch (final AwsServiceException e) {
            /*
             * While the handler contract states that the handler must always return a progress event,
             * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
             * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
             * to more specific error codes
             */
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    /**
     * If deletion of your resource requires some form of stabilization (e.g. propagation delay)
     * for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
     * @param awsRequest the aws service request to delete a resource
     * @param awsResponse the aws service response to delete a resource
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnDelete(
        final AwsRequest awsRequest,
        final AwsResponse awsResponse,
        final ProxyClient<SdkClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {

        // TODO: put your stabilization code here

        final boolean stabilized = true;
        logger.log(String.format("%s [%s] deletion has stabilized: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized));
        return stabilized;
    }
}
