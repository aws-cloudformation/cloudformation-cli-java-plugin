package {{ package_name }};

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

  // request to create a resource
  static AwsRequest translateToCreateRequest(final ResourceModel model) {
    return AwsRequest.builder()
        .propertiesToCreate(model.getProperties())
        .build();
  }

  // request to read a resource
  static AwsRequest translateToReadRequest(final ResourceModel model) {
    return AwsRequest.builder()
        .primaryIdentifier(model.getPrimaryIdentifier())
        .build();
  }

  // request to update properties of a previously created resource
  static AwsRequest translateToUpdateRequest(final ResourceModel model) {
    return AwsRequest.builder()
        .propertiesToUpdate(model.getSomeProperties())
        .build();
  }

  // request to delete a resource
  static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    return AwsRequest.builder()
        .primaryIdentifier(model.getPrimaryIdentifier())
        .build();
  }

  // translates resource object from sdk into a resource model
  static ResourceModel translateFromReadRequest(final AwsResponse response) {
    return ResourceModel.builder()
        .primaryIdentifier(response.primaryIdentifier())
        .someProperty(response.property())
        .build();
  }

  // translates resource objects from sdk into a resource model (primary identifier only)
  static List<ResourceModel> translateFromListRequest(final AwsResponse response) {
    return streamOfOrEmpty(response.resources())
        .map(resource -> ResourceModel.builder()
            .primaryIdentifier(resource.primaryIdentifier())
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }
}
