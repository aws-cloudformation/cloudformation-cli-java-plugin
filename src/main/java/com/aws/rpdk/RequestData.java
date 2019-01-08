package com.aws.rpdk;

import lombok.Data;

import java.util.Map;

@Data
public class RequestData<T> {
    private final Map<String, Object> attributes;
    private final String logicalResourceId;
    private final String physicalResourceId;
    private final T resourceProperties;
    private final T previousResourceProperties;
    private final Credentials credentials;
    private final Map<String, String> systemTags;
    private final Map<String, String> stackTags;
    private final Map<String, String> previousStackTags;
}
