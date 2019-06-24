/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the &quot;License&quot;).
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the &quot;license&quot; file accompanying this file. This file is distributed
* on an &quot;AS IS&quot; BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.cloudformation.proxy;

/**
 * Interface used to abstract the function of reporting back provisioning
 * progress to the handler caller
 */
public interface CallbackAdapter<T> {

    /**
     * On Lambda re-invoke we need to supply a new set of client credentials so this
     * function must be called whenever credentials are refreshed/changed in the
     * owning entity
     */
    void refreshClient();

    /**
     * Proxies a callback to the service entity which invoked this provisioning
     * request
     *
     * @param bearerToken unique identifier for this provisioning operation
     * @param errorCode (optional) error code in case of fault
     * @param operationStatus current status of provisioning operation
     * @param resourceModel the current state of the provisioned resource
     * @param statusMessage (optional) progress status which may be shown to end
     *            user
     */
    void reportProgress(String bearerToken,
                        HandlerErrorCode errorCode,
                        OperationStatus operationStatus,
                        T resourceModel,
                        String statusMessage);
}
