package com.amazonaws.cloudformation.proxy;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This can be used by Read, Create, Update and Delete handlers when invoking AWS services.
 * Each CallChain when invoking a service call explicitly provide a call graph name. This name
 * is used as a key prefix to memoize request, responses and results in each call sequences.
 * This {@link CallChain} provides a fluent API * design that ensure that right sequence of
 * steps is followed for making a service call.
 *
 * {@code
 *     public class CreateHandler extends BaseHandler<CallbackContext> {
 *
 *        @Override
 *        public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
 *            final AmazonWebServicesClientProxy proxy,
 *            final ResourceHandlerRequest<ResourceModel> request,
 *            final CallbackContext callbackContext,
 *            final Logger logger) {
 *
 *            final ProxyClient<KinesisClient> client = proxy.newProxy(KinesisClient::create);
 *            final ResourceModel model = request.getDesiredResourceState();
 *            ProgressEvent<ResourceModel, CallbackContext> created = proxy.initiate(
 *                "kinesis:CreateStream",
 *                client,
 *                model,
 *                callbackContext)
 *
 *                //
 *                // create request for a new stream
 *                //
 *                .request(
 *                    (m) -> CreateStreamRequest.builder()
 *                          .streamName(m.getName())
 *                          .shardCount(m.getShardCount()).build()
 *                )
 *
 *                //
 *                // Making the call via injection of credentials to make scoped credentials work
 *                //
 *                .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createStream))
 *
 *                //
 *                // Currently any failure to stabilize will be propagated over as failure to create. This means that
 *                // there is a likely hood that the stream could have been created but we timed out. Any attempt to re-create
 *                // this resource will fail with an already exists stream name. The stabiliserCreate provides the
 *                // ARN in the model (side effect, maybe change model later to be functional) from the describe calls.
 *                // So exceptions during stabilizes with report event with FAILED, but the model has the ARN to
 *                // indicate successful creation. So CFN can call us back with DELETE correctly.
 *                // IMP: if we do not have read permissions during create, this will fail causing the resources to
 *                // leak.
 *                //
 *                .stabilize(
 *                    new Delay.Exponential(2, 2^5, TimeUnit.SECONDS),
 *                    this::stabilizeCreate)
 *                .done(CallChain.Callback.progress());
 *
 *          }
 *     }
 * }
 *
 * Any service call should use {@link AmazonWebServicesClientProxy}. Here is the minimum sequence for the calls.
 *
 * <ol>
 *     <li>{@link CallChain#initiate(String, ProxyClient, Object, StdCallbackContext)}
 *     <li>{@link RequestMaker#request(Function)}
 *     <li>{@link Caller#call(BiFunction)}
 *     <li>{@link Completed#done(Function)}
 * </ol>
 *
 * @see AmazonWebServicesClientProxy
 */
public interface CallChain {

    /**
     * Each service call must be first initiated. Every call is provided a separate name
     * called call graph. This is eseential from both a tracing perspective as well as
     * {@link StdCallbackContext} automated replay capabilities.
     *
     * @param callGraph, the name of the service operation this call graph is about.
     * @param client, actual client needed to make the call wrapped inside {@link ProxyClient} to support
     *                injection of scoped credentials
     * @param model, the actual resource model that defines the shape for setting up this resource type.
     * @param cxt, Callback context used for supporting replay and dedupe capabilities.
     * @param <ClientT> Actual client e.g. KinesisClient.
     * @param <ModelT> The type (POJO) of Resource model.
     * @param <CallbackT>, callback context the extends {@link StdCallbackContext}
     * @return Provides the next logical set in the fluent API.
     */
    <ClientT, ModelT, CallbackT extends StdCallbackContext>
        RequestMaker<ClientT, ModelT, CallbackT> initiate(String callGraph,
                                                          ProxyClient<ClientT> client,
                                                          ModelT model,
                                                          CallbackT cxt);

    /**
     * This performs the translate step between the ModelT properties and what is needed for
     * making the service call.
     * @param <ClientT> Actual client e.g. KinesisClient.
     * @param <ModelT> The type (POJO) of Resource model.
     * @param <CallbackT>, callback context the extends {@link StdCallbackContext}
     */
    interface RequestMaker<ClientT, ModelT, CallbackT extends StdCallbackContext> {
        /**
         * Take a reference to the tranlater that take the resource model POJO as input and provide
         * a request object as needed to make the Service call.
         * @param maker, provide a functional transform from model to request object.
         * @param <RequestT>, the web service request created
         * @return returns the next step, to actually call the service.
         */
        <RequestT> Caller<RequestT, ClientT, ModelT, CallbackT> request(Function<ModelT, RequestT> maker);
    }

    /**
     * This Encapsulates the actual Call to the service that is being made via caller.
     * This allow for the proxy to intercept and wrap the caller in cases of replay and
     * provide the memoized response back
     * @param <RequestT>
     * @param <ClientT>
     * @param <ModelT>
     * @param <CallbackT>
     */
    interface Caller<RequestT,
                     ClientT,
                     ModelT,
                     CallbackT extends StdCallbackContext> {
        <ResponseT> Stabilizer<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            call(BiFunction<RequestT, ProxyClient<ClientT>, ResponseT> caller);

        Caller<RequestT, ClientT, ModelT, CallbackT> retry(Delay delay);
    }

    /**
     * All service calls made will use the same call back interface for handling
     * both exceptions as well as actual response received from the call. The
     * ResponseT is either the actual response result in the case of success or
     * the Exception thrown in the case of faults.
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working against
     * @param <CallbackT>, the callback context that contains results
     * @param <ReturnT>, the return from the callback.
     *
     * @see Exceptional
     */
    @FunctionalInterface
    interface Callback<RequestT,
                       ResponseT,
                       ClientT,
                       ModelT,
                       CallbackT extends StdCallbackContext,
                       ReturnT> {
        ReturnT
            invoke(RequestT request,
                   ResponseT response,
                   ProxyClient<ClientT> client,
                   ModelT model,
                   CallbackT context);

        static <RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext>
            Callback<RequestT,
                    ResponseT,
                    ClientT,
                    ModelT,
                    CallbackT,
                    ProgressEvent<ModelT, CallbackT>> progress() {
            return (r1, r2, c1, m1, c2) -> ProgressEvent.progress(m1, c2);
        }

        static <RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext>
            Callback<RequestT,
                ResponseT,
                ClientT,
                ModelT,
                CallbackT,
                ProgressEvent<ModelT, CallbackT>> success() {
            return (r1, r2, c1, m1, c2) -> ProgressEvent.success(m1, c2);
        }
    }

    /**
     * This provide the handler with the option to provide an explicit exception handler
     * that would have service exceptions that was received.
     *
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working against
     * @param <CallbackT>, the callback context that contains results
     */
    interface Exceptional<RequestT,
                          ResponseT,
                          ClientT,
                          ModelT,
                          CallbackT extends StdCallbackContext> extends Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>  {
        /**
         * @param handler, a predicate lambda expression that take the web request, response,
         *                 client, model and context and says continue or fail operation
         * @return true of you want to attempt another retry of the operation. false to indicate
         *          propagate error/fault.
         */
        Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            exceptFilter(Callback<? super RequestT, Exception, ClientT, ModelT, CallbackT, Boolean> handler);

        /**
         * @param handler, a lambda expression that take the web request, response,
         *                 client, model and context and says continue or fail operation by
         *                 providing the appropriate {@link ProgressEvent} back.
         * @return {@link ProgressEvent#getStatus()} is {@link OperationStatus#IN_PROGRESS}
         *          we will attempt another retry. Otherwise failure is propagated.
         */
        Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            exceptHandler(Callback<? super RequestT, Exception, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> handler);
    }

    /**
     * This provides an optional stabilization function to be incorporate before we are
     * done with the actual web service request. This is useful to ensure that the web
     * request created a resource that takes time to be live or available before additional
     * properties can be set on it. E.g. when one creates a Kinesis stream is takes some
     * time before the stream is active to do other operations on it like set the
     * retention period.
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
     *         model.setArn(description.streamARN());
     *         return (description.streamStatus() == StreamStatus.ACTIVE);
     *     }
     * }
     *
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working against
     * @param <CallbackT>, the callback context that contains results
     */
    interface Stabilizer<RequestT,
                     ResponseT,
                     ClientT,
                     ModelT,
                     CallbackT extends StdCallbackContext> extends Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT> {

        /**
         * @param callback, the stabilize predicate that is called several times to determine success.
         * @return true if the condition of stabilization has been meet else false.
         */
        Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            stabilize(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean> callback);

    }

    /**
     * One the call sequence has completed successfully, this is called to provide the
     * progress event.
     *
     * @param <RequestT>, the web service request that was made
     * @param <ResponseT> the response or the fault (Exception) that needs to handled
     * @param <ClientT>, the client that was used to invoke
     * @param <ModelT>, the resource model object that we are currently working against
     * @param <CallbackT>, the callback context that contains results
     */
    interface Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext> {
        /**
         * @param func, this works with only the response of the web service call to provide
         *              {@link ProgressEvent} function
         * @return {@link ProgressEvent} for successful web call.
         */
        ProgressEvent<ModelT, CallbackT>
            done(Function<ResponseT, ProgressEvent<ModelT, CallbackT>> func);

        /**
         * @param callback, similar to above function can make additional calls etc. to return the
         *                  {@link ProgressEvent}
         * @return {@link ProgressEvent} for a successful web call
         */
        ProgressEvent<ModelT, CallbackT>
            done(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> callback);

        /**
         * Helper function that provides a {@link OperationStatus#SUCCESS} status when the callchain is done
         */
        default ProgressEvent<ModelT, CallbackT> success() {
            return done(
                (request, response, client, model, context) -> ProgressEvent.success(model, context));
        }

        /**
         * Helper function that provides a {@link OperationStatus#IN_PROGRESS} status when the callchain is done
         */
        default ProgressEvent<ModelT, CallbackT> progress() {
            return progress(0);
        }

        /**
         * Helper function that provides a {@link OperationStatus#IN_PROGRESS} status when the callchain is done
         */
        default ProgressEvent<ModelT, CallbackT> progress(int callbackDelay) {
            return done(
                (request, response, client, model, context) -> ProgressEvent.defaultInProgressHandler(
                    context, callbackDelay, model)
            );
        }
    }

}
