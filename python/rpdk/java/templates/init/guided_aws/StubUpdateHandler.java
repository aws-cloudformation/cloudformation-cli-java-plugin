package {{ package_name }};

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
//import com.amzn.my.resource.ClientBuilder.SdkClient;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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


        // STEP 1 [initialize a proxy context]
        return proxy.initiate(CALL_GRAPH_VALUE, proxyClient, model, callbackContext)

            // STEP 2 [TODO: construct a body of a request]
            .request(Translator::translateToUpdateRequest)

            // STEP 3 [TODO: make an api call]
            .call(this::updateResource)

            // STEP 4 [TODO: stabilize is describing the resource until it is in a certain status]
            .stabilize(this::stabilizedOnUpdate)
            .progress()

            // STEP 5 [TODO: post stabilization update]
            .then(progress -> postUpdate(progress, proxyClient))

            // STEP 6 [TODO: describe call/chain to return the resource model]
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private AwsResponse updateResource(
        final AwsRequest awsRequest,
        final ProxyClient<SdkClient> proxyClient) {
        AwsResponse awsResponse = null;
        try {
            // TODO: add custom create resource logic
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/UpdateHandler.java#L69-L74

        // Framework handles the majority of standardized aws exceptions
        // Example of error handling in case of a non-standardized exception
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    /**
     * To account for eventual consistency, ensure you stabilize your update request so that a
     * subsequent Read will successfully describe the resource state that was provisioned
     * @param awsRequest the aws service request to update a resource
     * @param awsResponse the aws service  update resource response
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnUpdate(
        final AwsRequest awsRequest,
        final AwsResponse awsResponse,
        final ProxyClient<SdkClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {

        // TODO: add custom stabilization logic

        final boolean stabilized = true;

        logger.log(String.format("%s [%s] has been successfully stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return stabilized;
    }

    /**
     * If your resource is provisioned through multiple API calls, you will need to apply each subsequent update
     * step in a discrete call/stabilize chain to ensure the entire resource is provisioned as intended.
     * @param progressEvent of the previous state indicating success, in progress with delay callback or failed state
     * @param proxyClient the aws service client to make the call
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> postUpdate(
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
        final ProxyClient<SdkClient> proxyClient) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        try {
            // TODO: add custom create resource logic
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/UpdateHandler.java#L69-L74

        // Framework handles the majority of standardized aws exceptions
        // Example of error handling in case of a non-standardized exception
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return ProgressEvent.progress(model, callbackContext);
    }
}
