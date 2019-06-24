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
    void reportProgress(final String bearerToken,
                        final HandlerErrorCode errorCode,
                        final OperationStatus operationStatus,
                        final T resourceModel,
                        final String statusMessage);
}
