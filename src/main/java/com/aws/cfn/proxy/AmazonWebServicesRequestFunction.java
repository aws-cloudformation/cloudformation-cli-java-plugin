package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;

@FunctionalInterface
public interface AmazonWebServicesRequestFunction<RequestT extends AmazonWebServiceRequest,
                                                  ResultT extends AmazonWebServiceResult<ResponseMetadata>> {

    /**
     * Applies this function to the given arguments.
     *
     * @param request the function request argument
     * @return the function result
     */
    ResultT apply(RequestT request);
}
