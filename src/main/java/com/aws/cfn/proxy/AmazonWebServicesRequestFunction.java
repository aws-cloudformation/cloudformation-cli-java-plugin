package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;

@FunctionalInterface
public interface AmazonWebServicesRequestFunction<R extends AmazonWebServiceRequest,
                                                  O extends AmazonWebServiceResult<ResponseMetadata>> {

    /**
     * Applies this function to the given arguments.
     *
     * @param r the function request argument
     * @return the function result
     */
    O apply(R r);
}
