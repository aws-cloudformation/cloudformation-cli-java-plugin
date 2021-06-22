package software.amazon.cloudformation.exceptions;

import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class CfnInvalidTypeConfigurationException extends BaseHandlerException {

    private static final long serialVersionUID = -1646136434112354328L;

    public CfnInvalidTypeConfigurationException(String resourceTypeName, String message) {
        super(String.format(HandlerErrorCode.InvalidTypeConfiguration.getMessage(), resourceTypeName, message),
            null, HandlerErrorCode.InvalidTypeConfiguration);
    }
}
