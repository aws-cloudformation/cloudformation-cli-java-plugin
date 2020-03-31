package {{ package_name }};

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkClient;

public class ClientBuilder {
  // sample client builder - change SampleSdkClient on the desired one
  public static SampleSdkClient getClient() {
    return new SampleSdkClient();
  }

  // Sample code snippet - remove when implementing handlers
  public static class SampleSdkClient implements SdkClient {
    public AwsResponse executeRequest(final AwsRequest awsRequest) {
      return null;
    }

    @Override
    public String serviceName() {
      return null;
    }

    @Override
    public void close() {}
  }
}
