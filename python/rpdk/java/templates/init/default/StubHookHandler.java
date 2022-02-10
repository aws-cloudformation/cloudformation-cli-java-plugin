package {{ package_name }};

import com.google.common.collect.ImmutableSet;
import software.amazon.cloudformation.exceptions.UnsupportedTargetException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;

import java.util.Collection;

public class {{ operation }}HookHandler extends BaseHookHandler<CallbackContext, TypeConfigurationModel> {

    private static final Collection<String> HOOK_TARGET_NAMES = ImmutableSet.of(
        {% for target_name in target_names %}
        "{{target_name}}"{% if not loop.last %},{% endif %}

        {% endfor %}
    );

    @Override
    public ProgressEvent<HookTargetModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final HookHandlerRequest request,
        final CallbackContext callbackContext,
        final Logger logger,
        final TypeConfigurationModel typeConfiguration) {

        final String targetName = request.getHookContext().getTargetName();

        if (!HOOK_TARGET_NAMES.contains(targetName)) {
            throw new UnsupportedTargetException(targetName);
        }

        final String resultMessage = "Successfully invoked {{ operation }}HookHandler for target: " + targetName;

        return ProgressEvent.<HookTargetModel, CallbackContext>builder()
            .status(OperationStatus.SUCCESS)
            .message(resultMessage)
            .build();
    }
}
