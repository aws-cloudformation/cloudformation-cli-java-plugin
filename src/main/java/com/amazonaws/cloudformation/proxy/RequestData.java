package com.amazonaws.cloudformation.proxy;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class RequestData<ResourceT> {
    private Credentials callerCredentials;
    private Credentials platformCredentials;
    private Credentials resourceOwnerLoggingCredentials;
    private String resourceOwnerLogGroupName;
    private String logicalResourceId;
    private ResourceT resourceProperties;
    private ResourceT previousResourceProperties;
    private Map<String, String> systemTags;
    private Map<String, String> stackTags;
    private Map<String, String> previousStackTags;
}
