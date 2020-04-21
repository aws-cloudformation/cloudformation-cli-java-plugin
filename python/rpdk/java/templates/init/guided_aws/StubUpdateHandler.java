package {{ package_name }};

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

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

public class UpdateHandler extends BaseHandlerStd {
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
        return proxy.initiate("{{ call_graph }}::{{ operation }}", proxyClient, model, callbackContext)

            // STEP 2 [TODO: construct a body of a request]
            .translate(Translator::translateToUpdateRequest)

            // STEP 3 [TODO: make an api call]
            .call(this::updateResource)

            // STEP 4 [TODO: stabilize step is not necessarily required but typically involves describing the resource until it is in a certain status, though it can take many forms]
            // stabilization step may or may not be needed after each API call
            // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            .stabilize(this::stabilizedOnFirstUpdate)
            .progress()

            // If your resource is provisioned through multiple API calls, then the following pattern is required (and might take as many postUpdate callbacks as necessary)
            // STEP 5 [TODO: post stabilization update]
            .then(progress -> secondUpdate(progress, proxyClient))

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
            // TODO: put your update resource code here
            // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/commit/2077c92299aeb9a68ae8f4418b5e932b12a8b186#diff-c959c290785791b96fa4442573750fcdR69-R74

        } catch (final AwsServiceException e) {
            /*
             * While the handler contract states that the handler must always return a progress event,
             * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
             * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
             * to more specific error codes
             */
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    /**
     * If your resource requires some form of stabilization (e.g. service does not provide strong consistency), you will need to ensure that your code
     * accounts for any potential issues, so that a subsequent read/update requests will not cause any conflicts (e.g. NotFoundException/InvalidRequestException)
     * for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
     * @param awsResponse the aws service  update resource response
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnFirstUpdate(
        final AwsRequest awsRequest,
        final AwsResponse awsResponse,
        final ProxyClient<SdkClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {

        // TODO: put your stabilization code here

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
    private ProgressEvent<ResourceModel, CallbackContext> secondUpdate(
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
        final ProxyClient<SdkClient> proxyClient) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        try {
            // TODO: put your post update resource code here

        } catch (final AwsServiceException e) {
            /*
             * While the handler contract states that the handler must always return a progress event,
             * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
             * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
             * to more specific error codes
             */
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return ProgressEvent.progress(model, callbackContext);
    }
}
