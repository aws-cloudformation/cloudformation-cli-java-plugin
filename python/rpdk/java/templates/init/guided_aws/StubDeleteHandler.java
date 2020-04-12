package {{ package_name }};

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
//import com.amzn.my.resource.ClientBuilder.SdkClient;

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

public class {{ operation }}Handler extends BaseHandlerStd {
    // CallGraph value helps tracking the execution flow within the callback
    // TODO: change this if you care, or leave it
    private static final String CALL_GRAPH_VALUE = "{{ call_graph }}::{{ operation }}";
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


        return ProgressEvent.progress(model, callbackContext)

            // STEP 1 [check if resource already exists]
            // if target API does not support 'ResourceNotFoundException' then following check is required
            .then(progress -> checkForPreDeleteResourceExistence(proxy, request, progress))

            // STEP 2.0 [delete/stabilize progress chain - required for resource deletion]
            .then(progress ->
                // If your service API throws 'ResourceNotFoundException' for delete requests then DeleteHandler can return just proxy.initiate construction
                // STEP 2.0 [initialize a proxy context]
                proxy.initiate(CALL_GRAPH_VALUE, proxyClient, model, callbackContext)

                    // STEP 2.1 [TODO: construct a body of a request]
                    .request(Translator::translateToDeleteRequest)

                    // STEP 2.2 [TODO: make an api call]
                    .call(this::deleteResource)

                    // STEP 2.3 [TODO: stabilize is describing the resource until it is in a certain status]
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
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ReadHandler.java#L40-L46
            new ReadHandler().handleRequest(proxy, request, callbackContext, logger);
            return ProgressEvent.progress(model, callbackContext);
        } catch (CfnNotFoundException e) {
            logger.log(model.getPrimaryIdentifier() + " does not exist; creating the resource.");
            return ProgressEvent.success(model, callbackContext);
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
            // TODO: add custom create resource logic
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/DeleteHandler.java#L21-L27

        // Framework handles the majority of standardized aws exceptions
        // Example of error handling in case of a non-standardized exception
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    /**
     * If your service does not provide strong consistency, you will need to account for eventual consistency issues
     * ensure you stabilize your Delete request in order not to generate a false positive stack event
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

        // TODO: add custom stabilization logic

        // hint: if describe a resource throws ResourceNotFoundException, that might be good enough
        // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ReadHandler.java#L40-L46

        final boolean stabilized = true;
        logger.log(String.format("%s has successfully been deleted. Stabilized.", ResourceModel.TYPE_NAME));
        return stabilized;
    }
}
