package {{ package_name }};

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
        final AwsRequest awsRequest = null;
        // TODO: construct a request
        // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43

        return awsRequest;
    }

}
