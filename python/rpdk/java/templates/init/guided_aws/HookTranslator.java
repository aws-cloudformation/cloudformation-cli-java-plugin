package {{ package_name }};


import {{ package_name }}.model.my.example.resource.MyExampleResourceTargetModel;
import {{ package_name }}.model.other.example.resource.OtherExampleResourceTargetModel;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 */

public class Translator {

    /**
     * Request built from target model
     * @param targetModel target model
     * @return awsRequest the aws service request
     */
    static AwsRequest translateToRequest(final HookTargetModel targetModel) {
        final AwsRequest awsRequest;
        if (targetModel instanceof MyExampleResourceTargetModel) {
            awsRequest = translateToRequest((MyExampleResourceTargetModel) targetModel);
        } else if (targetModel instanceof OtherExampleResourceTargetModel) {
            awsRequest = translateToRequest((OtherExampleResourceTargetModel) targetModel);
        } else {
            // TODO: construct a request
            awsRequest = null;
        }

        return awsRequest;
    }

    static AwsRequest translateToRequest(final MyExampleResourceTargetModel targetModel) {
        final AwsRequest awsRequest = null;
        // TODO: construct a request
        return awsRequest;
    }

    static AwsRequest translateToRequest(final OtherExampleResourceTargetModel targetModel) {
        final AwsRequest awsRequest = null;
        // TODO: construct a request
        return awsRequest;
    }

}
