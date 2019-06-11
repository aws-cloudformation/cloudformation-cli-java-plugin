package com.amazonaws.cloudformation.proxy.service;

import org.joda.time.DateTime;

import java.util.Set;

@lombok.Data
@lombok.EqualsAndHashCode
@lombok.ToString
class Repository {
    private String repoName;
    private Set<String> users;
    private String arn;
    private DateTime created;
}
