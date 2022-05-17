package {{ package_name }};

import com.google.common.collect.ImmutableSet;
import {{ package_name }}.model.aws.s3.bucket.AwsS3Bucket;
import {{ package_name }}.model.aws.s3.bucket.AwsS3BucketTargetModel;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.hook.HookContext;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;
import software.amazon.cloudformation.proxy.hook.targetmodel.ResourceHookTargetModel;

import java.util.Collection;

public class {{ operation }}HookHandler extends BaseHookHandlerStd {

    private static final Collection<String> DO_NOT_DELETE_IDENTIFIERS = ImmutableSet.of(
            "DO_NOT_DELETE", "DO-NOT-DELETE", "do_not_delete", "do-not-delete"
    );

    @Override
    public ProgressEvent<HookTargetModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final HookHandlerRequest request,
            final CallbackContext callbackContext,
            final ProxyClient<SdkClient> proxyClient,
            final Logger logger,
            final TypeConfigurationModel typeConfiguration) {

        final HookContext hookContext = request.getHookContext();
        final String targetName = hookContext.getTargetName();

        logger.log(String.format("Successfully invoked {{ operation }}HookHandler for target %s.", targetName));

        final ResourceHookTargetModel<AwsS3Bucket> targetModel = hookContext.getTargetModel(AwsS3BucketTargetModel.class);

        final AwsS3Bucket resourceProperties = targetModel.getResourceProperties();

        final String bucketName = resourceProperties.getBucketName();
        for (final String identifier : DO_NOT_DELETE_IDENTIFIERS) {
            if (bucketName.contains(identifier)) {
                return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.NonCompliant)
                        .message(String.format("Bucket name contains a 'DO_NOT_DELETE' identifier: %s", bucketName))
                        .build();
            }
        }

        return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
