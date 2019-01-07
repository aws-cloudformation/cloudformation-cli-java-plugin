package com.aws.rpdk;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class RequestData<T> {
    private static final String ATTRIBUTES = "attributes";
    private static final String CREDENTIALS = "credentials";
    private static final String LOGICAL_RESOURCE_ID = "logicalResourceId";
    private static final String PHYSICAL_RESOURCE_ID = "physicalResourceId";
    private static final String RESOURCE_PROPERTIES = "resourceProperties";
    private static final String PREVIOUS_RESOURCE_PROPERTIES = "previousResourceProperties";
    private static final String SYSTEM_TAGS = "systemTags";
    private static final String STACK_TAGS = "stackTags";
    private static final String PREVIOUS_STACK_TAGS = "previousStackTags";

    @JsonProperty(ATTRIBUTES)
    private final Map<String, Object> attributes;

    @JsonProperty(LOGICAL_RESOURCE_ID)
    private final String logicalResourceId;

    @JsonProperty(PHYSICAL_RESOURCE_ID)
    private final String physicalResourceId;

    @JsonProperty(RESOURCE_PROPERTIES)
    private final T resourceProperties;

    @JsonProperty(PREVIOUS_RESOURCE_PROPERTIES)
    private final T previousResourceProperties;

    @JsonProperty(CREDENTIALS)
    private final Credentials credentials;

    @JsonProperty(SYSTEM_TAGS)
    private final Map<String, String> systemTags;

    @JsonProperty(STACK_TAGS)
    private final Map<String, String> stackTags;

    @JsonProperty(PREVIOUS_STACK_TAGS)
    private final Map<String, String> previousStackTags;
}
