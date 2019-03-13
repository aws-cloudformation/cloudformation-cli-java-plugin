package com.aws.cfn.proxy;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;

public interface AmazonWebServicesRequestFunction<C extends AmazonWebServiceClient,
                                                  R extends AmazonWebServiceRequest,
                                                  O extends AmazonWebServiceResult<ResponseMetadata>> {

    /**
     * Applies this function to the given arguments.
     *
     * @param c the AWS client to invoke the function
     * @param r the function request argument
     * @return the function result
     */
    O apply(C c, R r);
}
