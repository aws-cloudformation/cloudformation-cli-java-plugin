package {{ package_name }};

import {{ package_name }}.model.aws.s3.bucket.AwsS3Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookContext;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class {{ operation }}HookHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final {{ operation }}HookHandler handler = new {{ operation }}HookHandler();

        final TypeConfigurationModel typeConfiguration = TypeConfigurationModel.builder().build();

        final AwsS3Bucket resourceProperties = AwsS3Bucket.builder()
                .bucketName("MyBucket")
                .build();

        final Map<String, Object> targetModel = new HashMap<>();
        targetModel.put("ResourceProperties", resourceProperties);

        final HookHandlerRequest request = HookHandlerRequest.builder()
            .hookContext(HookContext.builder().targetName("AWS::S3::Bucket").targetModel(HookTargetModel.of(targetModel)).build())
            .build();

        final ProgressEvent<HookTargetModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger, typeConfiguration);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_nonCompliant() {
        final {{ operation }}HookHandler handler = new {{ operation }}HookHandler();

        final TypeConfigurationModel typeConfiguration = TypeConfigurationModel.builder().build();

        final AwsS3Bucket resourceProperties = AwsS3Bucket.builder()
                .bucketName("MY_BUCKET_DO_NOT_DELETE")
                .build();


        final Map<String, Object> targetModel = new HashMap<>();
        targetModel.put("ResourceProperties", resourceProperties);

        final HookHandlerRequest request = HookHandlerRequest.builder()
            .hookContext(HookContext.builder().targetName("AWS::S3::Bucket").targetModel(HookTargetModel.of(targetModel)).build())
            .build();

        final ProgressEvent<HookTargetModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger, typeConfiguration);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NonCompliant);
    }
}
