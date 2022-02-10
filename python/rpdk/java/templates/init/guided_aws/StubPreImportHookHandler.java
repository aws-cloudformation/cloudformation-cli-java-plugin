package {{ package_name }};

import {{ package_name }}.model.my.example.resource.MyExampleResourceTargetModel;
import {{ package_name }}.model.other.example.resource.OtherExampleResourceTargetModel;
import software.amazon.cloudformation.exceptions.UnsupportedTargetException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.hook.HookContext;
import software.amazon.cloudformation.proxy.hook.HookStatus;
import software.amazon.cloudformation.proxy.hook.HookProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;

public class {{ operation }}HookHandler extends BaseHookHandler<TypeConfigurationModel, CallbackContext> {

    @Override
    public HookProgressEvent<CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final HookHandlerRequest request,
        final CallbackContext callbackContext,
        final Logger logger,
        final TypeConfigurationModel typeConfiguration) {

        final HookContext hookContext = request.getHookContext();
        final String targetName = hookContext.getTargetName();
        logger.log(String.format("Successfully invoked {{ operation }}HookHandler for target %s.", targetName));

        final String expectedEncryptionAlgorithm = typeConfiguration.getEncryptionAlgorithm();
        logger.log(String.format("Verifying server side encryption for target %s, expecting target server side encryption algorithm to be %s.",
            targetName, expectedEncryptionAlgorithm));

        final String targetEncryptionAlgorithm;
        if ("My::Example::Resource".equals(targetName)) {
            targetEncryptionAlgorithm = (String) hookContext.getTargetModel(MyExampleResourceTargetModel.class).getResourceProperties().get("MyEncryptionAlgorithm");
        } else if ("Other::Example::Resource".equals(targetName)) {
            targetEncryptionAlgorithm = (String) hookContext.getTargetModel(OtherExampleResourceTargetModel.class).getResourceProperties().get("OtherEncryptionAlgorithm");
        } else {
            throw new UnsupportedTargetException(targetName);
        }

        if (targetEncryptionAlgorithm == null) {
            final String failureMessage = String.format("Failed to verify server side encryption for target %s, target does not have server side encryption enabled.",
                targetName);
            logger.log(failureMessage);

            return HookProgressEvent.<CallbackContext>builder()
                .status(HookStatus.FAILED)
                .message(failureMessage)
                .build();
        }

        if (!targetEncryptionAlgorithm.equals(expectedEncryptionAlgorithm)) {
            final String failureMessage = String.format("Failed to verify server side encryption for target %s, expecting encryption algorithm to be %s, acutal encryption algorithm is %s",
                targetName, expectedEncryptionAlgorithm, targetEncryptionAlgorithm);
            logger.log(failureMessage);

            return HookProgressEvent.<CallbackContext>builder()
                .status(HookStatus.FAILED)
                .message(failureMessage)
                .build();
        }

        final String successMessage = String.format("Successfully verified server side encryption for target %s.", targetName);
        return HookProgressEvent.<CallbackContext>builder()
            .status(HookStatus.SUCCESS)
            .message(successMessage)
            .build();
    }
}
