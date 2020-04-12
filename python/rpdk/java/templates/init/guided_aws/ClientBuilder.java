package {{ package_name }};

import software.amazon.cloudformation.LambdaWrapper;
import software.amazon.awssdk.core.SdkClient;

public class ClientBuilder {
  /*
  TODO: uncomment the following, replacing YourServiceAsyncClient with your service client name
  It is recommended to use static HTTP client so less memory is consumed
  e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ClientBuilder.java#L9

  private static class LazyHolder {
    static final YourServiceAsyncClient SERVICE_CLIENT = YourServiceAsyncClient.builder()
              .httpClient(LambdaWrapper.HTTP_CLIENT)
              .build();
  }

  public static YourServiceAsyncClient getClient() {
    return LazyHolder.SERVICE_CLIENT;
  }
  */

  // TODO: remove this implementation once you have uncommented the above
  public static SdkClient getClient() {
    return null;
  }
}
