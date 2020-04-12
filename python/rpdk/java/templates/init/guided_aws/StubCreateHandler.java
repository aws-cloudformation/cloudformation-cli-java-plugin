package {{ package_name }};

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
//import com.amzn.my.resource.ClientBuilder.SdkClient;

import java.util.Objects;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
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
            // if target API does not support 'ResourceAlreadyExistsException' then following check is required
            //.then(progress -> checkForPreCreateResourceExistence(proxy, request, progress))

            // STEP 2 [create/stabilize progress chain - required for resource creation]
            .then(progress ->
                // If your service API throws 'ResourceAlreadyExistsException' for create requests then CreateHandler can return just proxy.initiate construction
                // STEP 2.0 [initialize a proxy context]
                proxy.initiate(CALL_GRAPH_VALUE, proxyClient, model, callbackContext)

                    // STEP 2.1 [TODO: construct a body of a request]
                    .request(Translator::translateToCreateRequest)

                    // STEP 2.2 [TODO: make an api call]
                    .call(this::createResource)

                    // STEP 2.3 [TODO: stabilize is describing the resource until it is in a certain status]
                    .stabilize(this::stabilizedOnCreate)
                    .progress())

            // STEP 3 [TODO: describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * If your service API does not distinguish duplicate create requests against some identifier (e.g; resource Name)
     * and instead returns a 200 even though a resource already exists, you must first check if the resource exists here
     * NOTE: If your service API throws 'ResourceAlreadyExistsException' for create requests this method is not necessary
     * @param proxy Amazon webservice proxy to inject credentials correctly.
     * @param request incoming resource handler request
     * @param progressEvent event of the previous state indicating success, in progress with delay callback or failed state
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> checkForPreCreateResourceExistence(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        try {
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ReadHandler.java#L40-L46
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
     * @param awsRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private AwsResponse createResource(
        final AwsRequest awsRequest,
        final ProxyClient<SdkClient> proxyClient) {
        AwsResponse awsResponse = null;
        try {
            // TODO: add custom create resource logic
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/CreateHandler.java#L37-L43

        // Proxy framework handles the majority of standardized aws exceptions
        // Example of error handling in case of a non-standardized exception
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    /**
     * If your service does not provide strong consistency, you will need to account for eventual consistency issues
     * ensure you stabilize your Create request so that a subsequent Read will successfully describe the resource state that was provisioned
     * @param awsRequest the aws service request to create a resource
     * @param awsResponse the aws service response to create a resource
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnCreate(
        final AwsRequest awsRequest,
        final AwsResponse awsResponse,
        final ProxyClient<SdkClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {

        // TODO: add custom stabilization logic

        // hint: for some resources just describing current status would be enough
        // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ReadHandler.java#L40-L46

        final boolean stabilized = true;
        logger.log(String.format("%s [%s] has been stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return stabilized;
    }
}
