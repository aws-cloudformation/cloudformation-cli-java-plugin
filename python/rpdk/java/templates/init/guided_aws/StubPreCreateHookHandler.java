package {{ package_name }};

import {{ package_name }}.model.my.example.resource.MyExampleResource;
import {{ package_name }}.model.my.example.resource.MyExampleResourceTargetModel;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.cloudformation.exceptions.UnsupportedTargetException;
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

        if (!"My::Example::Resource".equals(targetName)) {
            throw new UnsupportedTargetException(targetName);
        }

        logger.log(String.format("Successfully invoked {{ operation }}HookHandler for target %s.", targetName));

        final String expectedEncryptionAlgorithm = typeConfiguration.getEncryptionAlgorithm();
        logger.log(String.format("Verifying server side encryption for target %s, expecting target server side encryption algorithm to be %s.",
            targetName, expectedEncryptionAlgorithm));

        final ResourceHookTargetModel<MyExampleResource> targetModel = hookContext.getTargetModel(MyExampleResourceTargetModel.class);

        final MyExampleResource resourceProperties = targetModel.getResourceProperties();
        final String targetEncryptionAlgorithm = (String) resourceProperties.get("MyEncryptionAlgorithm");

        if (targetEncryptionAlgorithm == null) {
            final String failureMessage = String.format("Failed to verify server side encryption for target %s, target does not have server side encryption enabled.",
                targetName);
            logger.log(failureMessage);

            return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .message(failureMessage)
                .build();
        }

        if (!targetEncryptionAlgorithm.equals(expectedEncryptionAlgorithm)) {
            final String failureMessage = String.format("Failed to verify server side encryption for target %s, expecting encryption algorithm to be %s, acutal encryption algorithm is %s",
                targetName, expectedEncryptionAlgorithm, targetEncryptionAlgorithm);
            logger.log(failureMessage);

            return ProgressEvent.<HookTargetModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .message(failureMessage)
                .build();
        }

        final String successMessage = String.format("Successfully verified server side encryption for target %s.", targetName);
        return ProgressEvent.<HookTargetModel, CallbackContext>builder()
            .status(OperationStatus.SUCCESS)
            .message(successMessage)
            .build();
    }
}
