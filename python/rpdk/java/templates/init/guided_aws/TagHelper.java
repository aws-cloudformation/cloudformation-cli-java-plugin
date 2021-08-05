package software.amazon.cloudformation.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.sun.java.accessibility.util.Translator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkClient;
// TODO: Replace the Tag model below according to your resource
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;

public class TagTest {
    /**
     * convertToMap
     *
     * Converts a collection of Tag objects to a tag-name -> tag-value map.
     *
     * Note: Tag objects with null tag values will not be included in the output
     * map.
     *
     * @param tags Collection of tags to convert
     * @return Converted Map of tags
     */
    public static Map<String, String> convertToMap(final Collection<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyMap();
        }
        return tags.stream()
            .filter(tag -> tag.value() != null)
            .collect(Collectors.toMap(
                Tag::key,
                Tag::value,
                (oldValue, newValue) -> newValue));
    }

    /**
     * convertToSet
     *
     * Converts a tag map to a set of Tag objects.
     *
     * Note: Like convertToMap, convertToSet filters out value-less tag entries.
     *
     * @param tagMap Map of tags to convert
     * @return Set of Tag objects
     */
    public static Set<Tag> convertToSet(final Map<String, String> tagMap) {
        if (MapUtils.isEmpty(tagMap)) {
            return Collections.emptySet();
        }
        return tagMap.entrySet().stream()
            .filter(tag -> tag.getValue() != null)
            .map(tag -> Tag.builder()
                .key(tag.getKey())
                .value(tag.getValue())
                .build())
            .collect(Collectors.toSet());
    }

    /**
     * shouldUpdateTags
     *
     * Generate tags to put into resource creation request.
     * This includes user defined tags and system tags as well.
     */
    public final Map<String, String> generateTagsForCreate(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> tagMap = new HashMap<>();

        if (handlerRequest.getSystemTags() != null) {
            tagMap.putAll(handlerRequest.getSystemTags());
        }

        if (handlerRequest.getDesiredResourceTags() != null) {
            tagMap.putAll(handlerRequest.getDesiredResourceTags());
        }

        // TODO: get tags from resource model based on your tag property name
        // TODO: tagMap.putAll(convertToMap(resourceModel.getTags()));
        return Collections.unmodifiableMap(tagMap);
    }

    /**
     * shouldUpdateTags
     *
     * Determines whether user defined tags or system tags have been changed during update.
     */
    public final boolean shouldUpdateTags(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        return areResourceTagsChanging(resourceModel, handlerRequest) || areSystemTagsChanging(handlerRequest);
    }

    private boolean areResourceTagsChanging(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> currentTags = getCurrentTags(handlerRequest);
        final Map<String, String> desiredTags = getNewDesiredTags(resourceModel, handlerRequest);
        return ObjectUtils.notEqual(currentTags, desiredTags);
    }

    private Map<String, String> getCurrentTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> currentTags = handlerRequest.getPreviousResourceTags();
        // TODO: get tags from previous resource state based on your tag property name
        // TODO: currentTags.putAll(handlerRequest.getPreviousResourceState().getTags());
        return currentTags;
    }

    private Map<String, String> getNewDesiredTags(final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> desiredTags = handlerRequest.getDesiredResourceTags();
        // TODO: get tags from resource model based on your tag property name
        // TODO: desiredTags.putAll(convertToMap(resourceModel.getTags()));
        return desiredTags;
    }

    private boolean areSystemTagsChanging(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
        final Map<String, String> currentSystemTags = handlerRequest.getPreviousSystemTags();
        final Map<String, String> desiredSystemTags = handlerRequest.getSystemTags();

        return !Objects.equals(currentSystemTags, desiredSystemTags);
    }

    /**
     * getTagsToAdd
     *
     * Determines the tags the customer desired to define or redefine.
     */
    public Map<String, String> generateTagsToAdd(final Map<String, String> currentTags, final Map<String, String> desiredTags) {
        return desiredTags.entrySet().stream()
            .filter(e -> !currentTags.containsKey(e.getKey()) || !Objects.equals(currentTags.get(e.getKey()), e.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }

    /**
     * getTagsToRemove
     *
     * Determines the tags the customer desired to remove from the function.
     */
    public Set<String> generateTagsToRemove(final Map<String, String> currentTags, final Map<String, String> desiredTags) {
        final Set<String> desiredTagNames = desiredTags.keySet();

        return currentTags.keySet().stream()
            .filter(tagName -> !desiredTagNames.contains(tagName))
            .collect(Collectors.toSet());
    }

    /**
     * getTagsToAdd
     *
     * Determines the tags the customer desired to define or redefine.
     */
    public Set<Tag> generateTagsToAdd(final Set<Tag> currentTags, final Set<Tag> desiredTags) {
        return Sets.difference(new HashSet<>(desiredTags), new HashSet<>(currentTags));
    }

    /**
     * getTagsToRemove
     *
     * Determines the tags the customer desired to remove from the function.
     */
    public Set<Tag> generateTagsToRemove(final Set<Tag> currentTags, final Set<Tag> desiredTags) {
        return Sets.difference(new HashSet<>(currentTags), new HashSet<>(desiredTags));
    }

    /**
     * doUpdateTags
     *
     * This function updates the target resource's user defined tags. It first
     * calculate what tags to add and remove, and then calls TagResource/UntagResource
     * to achieve the customer's desired set of tags as specified in their CloudFormation template.
     *
     */
    public ProgressEvent<ResourceModel, CallbackContext> doUpdateTags(final AmazonWebServicesClientProxy proxy, final ProxyClient<SdkClient> serviceClient,
                                                                      final ResourceModel resourceModel, final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext, final Logger logger) {
        return ProgressEvent.progress(resourceModel, callbackContext)
            .then(progress -> {
                final Map<String, String> tagsToAdd = generateTagsToAdd(getCurrentTags(handlerRequest), getNewDesiredTags(resourceModel, handlerRequest));
                if (!tagsToAdd.isEmpty()) {
                    return tagResource(proxy, serviceClient, resourceModel, handlerRequest, callbackContext, tagsToAdd, logger);
                }
                return ProgressEvent.defaultInProgressHandler(callbackContext, 0, resourceModel);
            })
            .then(progress -> {
                final Set<String> tagsToRemove = generateTagsToRemove(getCurrentTags(handlerRequest), getNewDesiredTags(resourceModel, handlerRequest));
                if (!tagsToRemove.isEmpty()) {
                    return untagResource(proxy, serviceClient, resourceModel, handlerRequest, callbackContext, tagsToRemove, logger);
                }
                return ProgressEvent.defaultInProgressHandler(callbackContext, 0, resourceModel);
            });
    }

    /**
     * tagResource during update
     *
     * Calls the service:TagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext>
    tagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<SdkClient> serviceClient, final ResourceModel resourceModel,
                final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext, final Map<String, String> addedTags, final Logger logger) {
        // TODO: add log for adding tags to resources during update
        // e.g. logger.log(String.format("[UPDATE][IN PROGRESS] Going to add tags for ... resource: %s with AccountId: %s",
        // resourceModel.getResourceName(), handlerRequest.getAwsAccountId()));

        // TODO: change untagResource in the method to your service API according to your SDK
        return proxy.initiate("{{ call_graph }}::{{ operation }}", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model ->
                Translator.tagResourceRequest(model, addedTags))
            .makeServiceCall((request, client) -> {
                return (AwsResponse) null;
                // TODO: replace the return null with your invoke log to call tagResource API to add tags
                // e.g. proxy.injectCredentialsAndInvokeV2(request, client.client()::tagResource))
            })
            .progress();
    }

    /**
     * untagResource during update
     *
     * Calls the service:UntagResource API.
     */
    private ProgressEvent<ResourceModel, CallbackContext>
    untagResource(final AmazonWebServicesClientProxy proxy, final ProxyClient<SdkClient> serviceClient, final ResourceModel resourceModel,
                  final ResourceHandlerRequest<ResourceModel> handlerRequest, final CallbackContext callbackContext, final Set<String> removedTags, final Logger logger) {
        // TODO: add log for removing tags from resources during update
        // e.g. logger.log(String.format("[UPDATE][IN PROGRESS] Going to remove tags for ... resource: %s with AccountId: %s",
        // resourceModel.getResourceName(), handlerRequest.getAwsAccountId()));

        // TODO: change untagResource in the method to your service API according to your SDK
        return proxy.initiate("{{ call_graph }}::{{ operation }}", serviceClient, resourceModel, callbackContext)
            .translateToServiceRequest(model ->
                Translator.untagResourceRequest(model, removedTags))
            .makeServiceCall((request, client) -> {
                return (AwsResponse) null;
                // TODO: replace the return null with your invoke log to call untag API to remove tags
                // e.g. proxy.injectCredentialsAndInvokeV2(request, client.client()::untagResource)
            })
            .progress();
    }

}
