package {{ package_name }};

import {{ package_name }}.model.aws.s3.bucket.AwsS3Bucket;
import {{ package_name }}.model.aws.s3.bucket.AwsS3BucketTargetModel;
import {{ package_name }}.model.aws.s3.bucket.BucketEncryption;
import {{ package_name }}.model.aws.s3.bucket.ServerSideEncryptionByDefault;
import {{ package_name }}.model.aws.s3.bucket.ServerSideEncryptionRule;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.hook.HookContext;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;
import software.amazon.cloudformation.proxy.hook.targetmodel.ResourceHookTargetModel;

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

        final String expectedEncryptionAlgorithm = typeConfiguration.getEncryptionAlgorithm();
        logger.log(String.format("Verifying server side encryption for target %s, expecting target server side encryption algorithm to be %s.",
            targetName, expectedEncryptionAlgorithm));

        final ResourceHookTargetModel<AwsS3Bucket> targetModel = hookContext.getTargetModel(AwsS3BucketTargetModel.class);

        final AwsS3Bucket resourceProperties = targetModel.getResourceProperties();

        final String bucketName = resourceProperties.getBucketName();

        final BucketEncryption bucketEncryption = resourceProperties.getBucketEncryption();
        if (bucketEncryption == null) {
            String failureMessage = String.format("Bucket Encryption not configured for bucket: %s", bucketName);
            logger.log(failureMessage);

            return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NonCompliant)
                    .message(failureMessage)
                    .build();
        }

        if (bucketEncryption.getServerSideEncryptionConfiguration() == null || bucketEncryption.getServerSideEncryptionConfiguration().isEmpty()) {
            String failureMessage = String.format("Server side encryption not configured for bucket: %s", bucketName);
            logger.log(failureMessage);

            return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NonCompliant)
                    .message(failureMessage)
                    .build();
        }

        for (final ServerSideEncryptionRule rule : bucketEncryption.getServerSideEncryptionConfiguration()) {
            final ServerSideEncryptionByDefault encryption = rule.getServerSideEncryptionByDefault();
            if (encryption == null || encryption.getSSEAlgorithm() == null) {
                final String failureMessage = String.format("Failed to verify server side encryption for target %s, target does not have server side encryption enabled.", targetName);
                logger.log(failureMessage);

                return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.NonCompliant)
                        .message(failureMessage)
                        .build();
            }

            if (!expectedEncryptionAlgorithm.equals(encryption.getSSEAlgorithm())) {
                final String failureMessage = String.format("Failed to verify server side encryption for target %s, expecting encryption algorithm to be %s, actual encryption algorithm is %s",
                        targetName, expectedEncryptionAlgorithm, encryption.getSSEAlgorithm());
                logger.log(failureMessage);

                return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                        .status(OperationStatus.FAILED)
                        .errorCode(HandlerErrorCode.NonCompliant)
                        .message(failureMessage)
                        .build();
            }

        }

        final String successMessage = String.format("Successfully verified server side encryption for target %s.", targetName);
        return ProgressEvent.<HookTargetModel, CallbackContext>builder()
            .status(OperationStatus.SUCCESS)
            .message(successMessage)
            .build();
    }
}
