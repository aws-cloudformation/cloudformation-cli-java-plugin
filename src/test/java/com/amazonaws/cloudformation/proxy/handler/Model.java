package com.amazonaws.cloudformation.proxy.handler;

import org.joda.time.DateTime;

import java.util.Set;

@lombok.Builder
@lombok.Data
@lombok.EqualsAndHashCode
@lombok.ToString
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Model {
    private String repoName;
    private Set<String> users;
    private String arn;
    private DateTime created;
}
