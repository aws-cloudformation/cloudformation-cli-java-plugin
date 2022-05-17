package {{ package_name }};

import {{ package_name }}.model.aws.s3.bucket.AwsS3Bucket;
import {{ package_name }}.model.aws.s3.bucket.AwsS3BucketTargetModel;
import {{ package_name }}.model.aws.s3.bucket.BucketEncryption;
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

import java.util.Objects;

public class {{ operation }}HookHandler extends BaseHookHandlerStd {

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

        final AwsS3Bucket previousResourceProperties = targetModel.getPreviousResourceProperties();
        final AwsS3Bucket resourceProperties = targetModel.getResourceProperties();

        logger.log(String.format("Verifying encryption has not been modified for bucket: %s", resourceProperties.getBucketName()));

        final BucketEncryption previousBucketEncryption = previousResourceProperties.getBucketEncryption();
        final BucketEncryption bucketEncryption = resourceProperties.getBucketEncryption();

        if (Objects.equals(bucketEncryption, previousBucketEncryption)) {
            return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                    .status(OperationStatus.SUCCESS)
                    .build();
        } else {
            return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NonCompliant)
                    .message(String.format("Encryption has been modified for bucket: %s", resourceProperties.getBucketName()))
                    .build();
        }
    }
}
