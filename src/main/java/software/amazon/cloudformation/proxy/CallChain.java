/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.proxy;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This can be used by Read, Create, Update and Delete handlers when invoking
 * AWS services. Each CallChain when invoking a service call explicitly provide
 * a call graph name. This name is used as a key prefix to memoize request,
 * responses and results in each call sequences. This {@link CallChain} provides
 * a fluent API * design that ensure that right sequence of steps is followed
 * for making a service call.
 *
 *
 * Any service call should use {@link AmazonWebServicesClientProxy}. Here is the
 * minimum sequence for the calls.
 *
 * <ol>
 * <li>{@link CallChain#initiate(String, ProxyClient, Object, StdCallbackContext)}
 * <li>{@link RequestMaker#translateToServiceRequest(Function)}
 * <li>{@link Caller#makeServiceCall(BiFunction)}
 * <li>{@link Completed#done(Function)}
 * </ol>
 *
 * @see AmazonWebServicesClientProxy
 */
public interface CallChain {

    /**
     * Provides an API initiator interface that works for all API calls that need
     * conversion, retry-backoff strategy, common exception handling and more
     * against desired state of the resource and callback context. This needs to be
     * instantiated once with the client, model and callback context and used. It
     * takes a reference to the desired state model, so any changes made on the
     * model object will be reflected here. The model and callback can accessed from
     * the instantiator instance
     *
     * @param <ClientT> the AWS Service client.
     * @param <ModelT> the model object being worked on
     * @param <CallbackT> the callback context
     */
    interface Initiator<ClientT, ModelT, CallbackT extends StdCallbackContext> extends RequestMaker<ClientT, ModelT, CallbackT> {
        /**
         * Each service call must be first initiated. Every call is provided a separate
         * name called call graph. This is essential from both a tracing perspective as
         * well as {@link StdCallbackContext} automated replay capabilities.
         *
         * @param callGraph, the name of the service operation this call graph is about.
         * @return Provides the next logical set in the fluent API.
         */
        RequestMaker<ClientT, ModelT, CallbackT> initiate(String callGraph);

        /**
         * @return the model associated with the API initiator. Can not be null
         */
        ModelT getResourceModel();

        /**
         * @return the callback context associated with API initiator, Can not be null
         */
        CallbackT getCallbackContext();

        /**
         * @return logger associated to log messages
         */
        Logger getLogger();

        /**
         * Can rebind a new model to the call chain while retaining the client and
         * callback context
         *
         * @param model, the new model for the callchain initiation
         * @param <NewModelT>, this actual model type
         * @return new {@link Initiator} that now has the new model associated with it
         */
        <NewModelT> Initiator<ClientT, NewModelT, CallbackT> rebindModel(NewModelT model);

        /**
         * Can rebind a new callback context for a call chain while retaining the model
         * and client
         *
         * @param callback the new callback context
         * @param <NewCallbackT> new callback context type
         * @return new {@link Initiator} that now has the new callback associated with
         *         it
         */
        <NewCallbackT extends StdCallbackContext> Initiator<ClientT, ModelT, NewCallbackT> rebindCallback(NewCallbackT callback);
    }

    /**
     * factory method can created an {@link Initiator}
     *
     * @param client AWS Service Client. Recommend using Sync client as the
     *            framework handles interleaving as needed.
     * @param model the resource desired state model, usually
     * @param context callback context that tracks all outbound API calls
     * @param <ClientT> Actual client e.g. KinesisClient.
     * @param <ModelT> The type (POJO) of Resource model.
     * @param <CallbackT>, callback context the extends {@link StdCallbackContext}
     *
     * @return an instance of the {@link Initiator}
     */
    <ClientT, ModelT, CallbackT extends StdCallbackContext>
        Initiator<ClientT, ModelT, CallbackT>
        newInitiator(ProxyClient<ClientT> client, ModelT model, CallbackT context);

    /**
     * Each service call must be first initiated. Every call is provided a separate
     * name called call graph. This is eseential from both a tracing perspective as
     * well as {@link StdCallbackContext} automated replay capabilities.
     *
     * @param callGraph, the name of the service operation this call graph is about.
     * @param client, actual client needed to make the call wrapped inside
     *            {@link ProxyClient} to support injection of scoped credentials
     * @param model, the actual resource model that defines the shape for setting up
     *            this resource type.
     * @param cxt, Callback context used for supporting replay and dedupe
     *            capabilities.
     * @param <ClientT> Actual client e.g. KinesisClient.
     * @param <ModelT> The type (POJO) of Resource model.
     * @param <CallbackT>, callback context the extends {@link StdCallbackContext}
     * @return Provides the next logical set in the fluent API.
     */
    <ClientT, ModelT, CallbackT extends StdCallbackContext>
        RequestMaker<ClientT, ModelT, CallbackT>
        initiate(String callGraph, ProxyClient<ClientT> client, ModelT model, CallbackT cxt);

    /**
     * This performs the translate step between the ModelT properties and what is
     * needed for making the service call.
     *
     * @param <ClientT> Actual client e.g. KinesisClient.
     * @param <ModelT> The type (POJO) of Resource model.
     * @param <CallbackT>, callback context the extends {@link StdCallbackContext}
     */
    interface RequestMaker<ClientT, ModelT, CallbackT extends StdCallbackContext> {
        /**
         * use {@link #translateToServiceRequest(Function)}
         *
         * Take a reference to the tranlater that take the resource model POJO as input
         * and provide a request object as needed to make the Service call.
         *
         * @param maker, provide a functional transform from model to request object.
         * @param <RequestT>, the web service request created
         * @return returns the next step, to actually call the service.
         */
        @Deprecated
        default <RequestT> Caller<RequestT, ClientT, ModelT, CallbackT> request(Function<ModelT, RequestT> maker) {
            return translateToServiceRequest(maker);
        }

        /**
         * Take a reference to the tranlater that take the resource model POJO as input
         * and provide a request object as needed to make the Service call.
         *
         * @param maker, provide a functional transform from model to request object.
         * @param <RequestT>, the web service request created
         * @return returns the next step, to actually call the service.
         */
        <RequestT> Caller<RequestT, ClientT, ModelT, CallbackT> translateToServiceRequest(Function<ModelT, RequestT> maker);
    }

    /**
     * This Encapsulates the actual Call to the service that is being made via
     * caller. This allow for the proxy to intercept and wrap the caller in cases of
     * replay and provide the memoized response back
     *
     * @param <RequestT>, the AWS serivce request we are making
     * @param <ClientT>, the web service client to make the call
     * @param <ModelT>, the current model we are using
     * @param <CallbackT>, the callback context for handling all AWS service request
     *            responses
     */
    interface Caller<RequestT, ClientT, ModelT, CallbackT extends StdCallbackContext> {
        @Deprecated
        default <ResponseT>
            Stabilizer<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            call(BiFunction<RequestT, ProxyClient<ClientT>, ResponseT> caller) {
            return makeServiceCall(caller);
        }

        <ResponseT>
            Stabilizer<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            makeServiceCall(BiFunction<RequestT, ProxyClient<ClientT>, ResponseT> caller);

        @Deprecated
        default Caller<RequestT, ClientT, ModelT, CallbackT> retry(Delay delay) {
            return backoffDelay(delay);
        }

        Caller<RequestT, ClientT, ModelT, CallbackT> backoffDelay(Delay delay);

    }

    /**
     * All service calls made will use the same call back interface for handling
     * both exceptions as well as actual response received from the call. The
     * ResponseT is either the actual response result in the case of success or the
     * Exception thrown in the case of faults.
     *
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to
     *            handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working
     *            against
     * @param <CallbackT>, the callback context that contains results
     * @param <ReturnT>, the return from the callback.
     *
     * @see Exceptional
     */
    @FunctionalInterface
    interface Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext, ReturnT> {
        ReturnT invoke(RequestT request, ResponseT response, ProxyClient<ClientT> client, ModelT model, CallbackT context);
    }

    /**
     * This provide the handler with the option to provide an explicit exception
     * handler that would have service exceptions that was received.
     *
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to
     *            handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working
     *            against
     * @param <CallbackT>, the callback context that contains results
     */
    interface Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext>
        extends Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT> {
        /**
         * @param handler, a predicate lambda expression that take the web request,
         *            response, client, model and context and says continue or fail
         *            operation
         * @return true of you want to attempt another retry of the operation. false to
         *         indicate propagate error/fault.
         */
        @Deprecated
        default Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            exceptFilter(Callback<? super RequestT, Exception, ClientT, ModelT, CallbackT, Boolean> handler) {
            return retryErrorFilter(handler);
        }

        /**
         * @param handler, a predicate lambda expression that takes the web request,
         *            exception, client, model and context to determine to retry the
         *            exception thrown by the service or fail operation. This is the
         *            simpler model then {@link #handleError(ExceptionPropagate)} for
         *            most common retry scenarios If we need more control over the
         *            outcome, then use {@link #handleError(ExceptionPropagate)}
         * @return true of you want to attempt another retry of the operation. false to
         *         indicate propagate error/fault.
         */
        Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            retryErrorFilter(Callback<? super RequestT, Exception, ClientT, ModelT, CallbackT, Boolean> handler);

        /**
         * @param handler, a lambda expression that takes the web request, response,
         *            client, model and context returns a successful or failed
         *            {@link ProgressEvent} back or can rethrow service exception to
         *            propagate errors. If handler needs to retry the exception, the it
         *            will throw a
         *            {@link software.amazon.awssdk.core.exception.RetryableException}
         * @return a ProgressEvent for the model
         */
        @Deprecated
        default Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT> exceptHandler(ExceptionPropagate<? super RequestT,
            Exception, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> handler) {
            return handleError(handler);
        }

        /**
         * @param handler, a lambda expression that take the web request, response,
         *            client, model and context and says continue or fail operation by
         *            providing the appropriate {@link ProgressEvent} back.
         * @return If status is {@link OperationStatus#IN_PROGRESS} we will attempt
         *         another retry. Otherwise failure is propagated.
         */
        Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT> handleError(ExceptionPropagate<? super RequestT, Exception,
            ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> handler);
    }

    /**
     * When implementing this interface, developers can either propagate the
     * exception as is. If the exception can be retried, throw
     * {@link software.amazon.awssdk.core.exception.RetryableException}
     *
     * @param <RequestT> the API request object
     * @param <E> the exception that is thrown by the API
     * @param <ClientT> the service client
     * @param <ModelT> current desired state resource model
     * @param <CallbackT> current callback context
     * @param <ReturnT> result object
     */
    @FunctionalInterface
    interface ExceptionPropagate<RequestT, E extends Exception, ClientT, ModelT, CallbackT extends StdCallbackContext, ReturnT> {
        ReturnT invoke(RequestT request, E exception, ProxyClient<ClientT> client, ModelT model, CallbackT context)
            throws Exception;
    }

    /**
     * This provides an optional stabilization function to be incorporate before we
     * are done with the actual web service request. This is useful to ensure that
     * the web request created a resource that takes time to be live or available
     * before additional properties can be set on it. E.g. when one creates a
     * Kinesis stream is takes some time before the stream is active to do other
     * operations on it like set the retention period.
     *
     * {@code
     *     private Boolean isStreamActive(
     *         CreateStreamRequest req,
     *         CreateStreamResponse res,
     *         ProxyClient<KinesisClient> client,
     *         ResourceModel model,
     *         CallbackContext cxt) {
     *
     *         DescribeStreamRequest r =
     *                 DescribeStreamRequest.builder().streamName(req.streamName()).build();
     *         DescribeStreamResponse dr = client.injectCredentialsAndInvokeV2(
     *             r, client.client()::describeStream);
     *         StreamDescription description = dr.streamDescription();
     *         model.setArn(description.streamARN()); return
     * (description.streamStatus() == StreamStatus.ACTIVE); } }
     *
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to
     *            handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working
     *            against
     * @param <CallbackT>, the callback context that contains results
     */
    interface Stabilizer<RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext>
        extends Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT> {

        /**
         * @param callback, the stabilize predicate that is called several times to
         *            determine success.
         * @return true if the condition of stabilization has been meet else false.
         */
        Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            stabilize(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean> callback);

    }

    /**
     * One the call sequence has completed successfully, this is called to provide
     * the progress event.
     *
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to
     *            handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working
     *            against
     * @param <CallbackT>, the callback context that contains results
     */
    interface Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext> {
        /**
         * @param func, this works with only the response of the web service call to
         *            provide {@link ProgressEvent} function
         * @return {@link ProgressEvent} for successful web call.
         */
        ProgressEvent<ModelT, CallbackT> done(Function<ResponseT, ProgressEvent<ModelT, CallbackT>> func);

        /**
         * @param callback, similar to above function can make additional calls etc. to
         *            return the {@link ProgressEvent}
         * @return {@link ProgressEvent} for a successful web call
         */
        ProgressEvent<ModelT, CallbackT>
            done(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> callback);

        /**
         * @return {@link ProgressEvent} Helper function that provides a
         *         {@link OperationStatus#SUCCESS} status when the callchain is done
         */
        default ProgressEvent<ModelT, CallbackT> success() {
            return done((request, response, client, model, context) -> ProgressEvent.success(model, context));
        }

        /**
         * @return {@link ProgressEvent} Helper function that provides a
         *         {@link OperationStatus#IN_PROGRESS} status when the callchain is done
         */
        default ProgressEvent<ModelT, CallbackT> progress() {
            return progress(0);
        }

        /**
         * @param callbackDelay the number of seconds to delay before calling back into
         *            this externally
         * @return {@link ProgressEvent} Helper function that provides a
         *         {@link OperationStatus#IN_PROGRESS} status when the callchain is done
         *         with callback delay
         */
        default ProgressEvent<ModelT, CallbackT> progress(int callbackDelay) {
            return done((request, response, client, model, context) -> ProgressEvent.defaultInProgressHandler(context,
                callbackDelay, model));
        }
    }

}
