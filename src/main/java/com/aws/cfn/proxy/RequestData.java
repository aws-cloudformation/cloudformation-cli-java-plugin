package com.aws.cfn.proxy;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class RequestData {
    private Credentials credentials;
    private String logicalResourceId;
    private Map<String, Object> resourceProperties;
    private Map<String, Object> previousResourceProperties;
    private Map<String, String> systemTags;
    private Map<String, String> stackTags;
    private Map<String, String> previousStackTags;
}
