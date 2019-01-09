package com.aws.rpdk;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class RequestData<T> {
    private Map<String, Object> attributes;
    private String logicalResourceId;
    private String physicalResourceId;
    private T resourceProperties;
    private T previousResourceProperties;
    private Credentials credentials;
    private Map<String, String> systemTags;
    private Map<String, String> stackTags;
    private Map<String, String> previousStackTags;
}
