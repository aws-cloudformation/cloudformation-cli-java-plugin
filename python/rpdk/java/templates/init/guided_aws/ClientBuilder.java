package {{ package_name }};

public class ClientBuilder {
  // sample client builder - change ServiceSdkClient on the desired one
  public static ServiceSdkClient getClient() {
    return ServiceSdkClient.create();
  }
}
