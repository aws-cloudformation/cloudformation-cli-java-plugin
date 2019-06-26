package com.amazonaws.cloudformation.proxy.service;

public interface ServiceClient {
    CreateResponse createRepository(CreateRequest r);
    DescribeResponse describeRepository(DescribeRequest r);
}
