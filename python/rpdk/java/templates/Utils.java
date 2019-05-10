// This is a generated file. Modifications will be overwritten.
package {{ package_name }};


@Data
@AllArgsConstructor
public class BaseConfiguration {
    public static final int GENERATED_PHYSICALID_MAXLEN = 40;

    /**
     * For named resources, use this method to safely generate a user friendly resource name when the customer does not pass in a name
     * For more info, see the named resources section of the Self Service developer guide
     * https://w.amazon.com/index.php/AWS21/Self_Service/Developer_Guide#Named_Resources
     * @return generated ID string
     */
    public String generateResourceId(final String logicalResourceId, final String clientRequestToken){
        return generatePhysicalResourceId(logicalResourceId, clientRequestToken, GENERATED_PHYSICALID_MAXLEN);
    }

    /**
     * Generate the resource physical ID with specified max length if necessary for the API
     * @return generated ID string
     */
    public String generateResourceId(final String logicalResourceId, final String clientRequestToken, final int maxLength){
        int maxLogicalIdLength = maxLength - (Constants.GUID_LENGTH + 1);
        int endIndex = logicalResourceId.length() > maxLogicalIdLength ? maxLogicalIdLength : logicalResourceId.length();
        StringBuilder sb = new StringBuilder();
        if (endIndex > 0) {
            sb.append(logicalResourceId.substring(0, endIndex)).append("-");
        }
        return sb.append(RandomStringUtils.random(Constants.GUID_LENGTH, 0, 0, true, true, null, new Random(clientRequestToken.hashCode())))
                .toString();
    }

    /**
     * This will result in the end of the resource work flow with no retrying
     * @return a failure ResourceResponse
     */
    public static ProgressEvent defaultFailureHandler(final ProgressEvent progressEvent, final Exception e, final HandlerErrorCode handlerErrorCode) {
        progressEvent.setMessage(e.getMessage());
        progressEvent.setStatus(OperationStatus.FAILED);
        progressEvent.setErrorCode(handlerErrorCode);

        return progressEvent;
    }

    /**
     * @return a successful ResourceResponse with resource Model
     */
    public static ProgressEvent defaultSuccessHandler(final ProgressEvent progressEvent, final ResourceModel resourceModel) {
        progressEvent.setStatus(OperationStatus.SUCCESS);
        progressEvent.setResourceModel(resourceModel);

        return progressEvent;
    }
}
