package com.amazonaws.cloudformation.proxy;

import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestData<ResourceT> {
    private Credentials callerCredentials;
    private Credentials platformCredentials;
    private String logicalResourceId;
    private ResourceT resourceProperties;
    private ResourceT previousResourceProperties;
    private Map<String, String> systemTags;
    private Map<String, String> stackTags;
    private Map<String, String> previousStackTags;
}
