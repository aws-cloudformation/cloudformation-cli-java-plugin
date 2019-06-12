package com.amazonaws.cloudformation.proxy.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Set;

@lombok.Builder
@lombok.Data
@lombok.EqualsAndHashCode
@lombok.ToString
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Model {
    public static final String TYPE_NAME = "AWS::Code::Repository";

    @JsonProperty("RepoName")
    private String repoName;
    @JsonProperty("Users")
    private Set<String> users;
    @JsonProperty("Arn")
    private String arn;
    @JsonProperty("Created")
    private Date created;
    @JsonProperty("AccessDenied")
    private Boolean accessDenied;
    @JsonProperty("Throttle")
    private Boolean throttle;
}
