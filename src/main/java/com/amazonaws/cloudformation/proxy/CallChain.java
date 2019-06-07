package com.amazonaws.cloudformation.proxy;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This can be used by Read, Create, Update and Delete handlers when invoking AWS services.
 * Each CallChain when invoking a service call explicitly has a proper context is stashed
 * key prefixed by call groph name for each operation. This call chain provide a fluent API
 * design that provides the sequence of steps that is needed from making a service call.
 *
 * {@code
 * public class CreateHandler extends BaseHandler<CallbackContext> {
 *
 *    private final KinesisClient actualClient = KinesisClient.create();
 *
 *    @Override
 *    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
 *        final AmazonWebServicesClientProxy proxy,
 *        final ResourceHandlerRequest<ResourceModel> request,
 *        final CallbackContext callbackContext,
 *        final Logger logger) {
 *
 *        final CallChain.ProxyClient<KinesisClient> client = proxy.newProxy(actualClient);
 *        final ResourceModel model = request.getDesiredResourceState();
 *        ProgressEvent<ResourceModel, CallbackContext> created = proxy.initiate(
 *            "Stream.Create",
 *            client,
 *            model,
 *            callbackContext)
 *
 *            //
 *            // create request for a new stream
 *            //
 *            .request(
 *                (m) -> CreateStreamRequest.builder()
 *                      .streamName(m.getName())
 *                      .shardCount(m.getShardCount()).build()
 *            )
 *
 *            //
 *            // Making the call via injection of credentials to make scoped credentials work
 *            //
 *            .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createStream))
 *
 *            //
 *            // Currently any failure to stabilize will be propagated over as failure to create. This means that
 *            // there is a likely hood that the stream could have been created but we timed out. Any attempt to re-create
 *            // this resource will fail with an already exists stream name. The stabiliserCreate provides the
 *            // ARN in the model (side effect, maybe change model later to be functional) from the describe calls.
 *            // So exceptions during stabilizes with report event with FAILED, but the model has the ARN to
 *            // indicate successful creation. So CFN can call us back with DELETE correctly.
 *            // IMP: if we do not have read permissions during create, this will fail causing the resources to
 *            // leak.
 *            //
 *            .stabilize(
 *                new Delay.Exponential(2, 2^5, TimeUnit.SECONDS),
 *                this::stabilizeCreate)
 *            .done(CallChain.Callback.progress());
 *
 *      }
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

    interface Caller<RequestT,
                     ClientT,
                     ModelT,
                     CallbackT extends StdCallbackContext> {
        <ResponseT> Stabilizer<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            call(BiFunction<RequestT, ProxyClient<ClientT>, ResponseT> caller);

        Caller<RequestT, ClientT, ModelT, CallbackT> retry(Delay delay);
    }

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

    interface Exceptional<RequestT,
                          ResponseT,
                          ClientT,
                          ModelT,
                          CallbackT extends StdCallbackContext> extends Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>  {
        Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            exceptFilter(Callback<RequestT, Exception, ClientT, ModelT, CallbackT, Boolean> handler);

        Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            exceptHandler(Callback<RequestT, Exception, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> handler);
    }

    interface Stabilizer<RequestT,
                     ResponseT,
                     ClientT,
                     ModelT,
                     CallbackT extends StdCallbackContext> extends Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT> {

        Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT>
            stabilize(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean> callback);

    }

    interface Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext> {
        ProgressEvent<ModelT, CallbackT>
            done(Function<ResponseT, ProgressEvent<ModelT, CallbackT>> func);
        ProgressEvent<ModelT, CallbackT>
            done(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> callback);
    }

}
