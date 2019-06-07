package com.amazonaws.cloudformation.proxy;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.base.Preconditions;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.NonRetryableException;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AmazonWebServicesClientProxy implements CallChain {

    public <ClientT> ProxyClient<ClientT> newProxy(@Nonnull ClientT client) {
        return new ProxyClient<ClientT>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
                ResponseT injectCredentialsAndInvokeV2(
                RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return AmazonWebServicesClientProxy.this.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
                CompletableFuture<ResponseT> injectCredentialsAndInvokeV2Aync(RequestT request,
                                                                              Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                return AmazonWebServicesClientProxy.this.injectCredentialsAndInvokeV2Async(request, requestFunction);
            }

            @Override
            public ClientT client() {
                return client;
            }
        };
    }

    @Override
    public <ClientT, ModelT, CallbackT extends StdCallbackContext> RequestMaker<ClientT, ModelT, CallbackT>
        initiate(String callGraph, ProxyClient<ClientT> client, ModelT model, CallbackT cxt) {
        return new CallContext<>(callGraph, client, model, cxt);
    }

    class CallContext<ClientT, ModelT, CallbackT extends StdCallbackContext>
        implements CallChain.RequestMaker<ClientT, ModelT, CallbackT> {

        private final String callGraph;
        private final ProxyClient<ClientT> client;
        private final ModelT model;
        private final CallbackT context;
        private Delay delay = new Delay.Fixed(3, 5, TimeUnit.SECONDS);
        CallContext(String callGraph, ProxyClient<ClientT> client, ModelT model, CallbackT context) {
            this.callGraph = Preconditions.checkNotNull(callGraph);
            this.client = Preconditions.checkNotNull(client);
            this.model = Preconditions.checkNotNull(model);
            this.context = context;
        }

        @Override
        public <RequestT> Caller<RequestT, ClientT, ModelT, CallbackT> request(Function<ModelT, RequestT> maker) {
            return new Caller<RequestT, ClientT, ModelT, CallbackT>() {

                @Override
                public Caller<RequestT, ClientT, ModelT, CallbackT> retry(Delay delay) {
                    CallContext.this.delay = delay;
                    return this;
                }

                @Override
                public <ResponseT> Stabilizer<RequestT, ResponseT, ClientT, ModelT, CallbackT>
                    call(BiFunction<RequestT, ProxyClient<ClientT>, ResponseT> caller) {
                    return new Stabilizer<RequestT, ResponseT, ClientT, ModelT, CallbackT>() {

                        private Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean> waitFor;
                        private Callback<RequestT, Exception, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> exceptHandler;

                        @Override
                        public Exceptional<RequestT, ResponseT, ClientT, ModelT, CallbackT>
                            stabilize(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean> callback) {
                            this.waitFor = callback;
                            return this;
                        }

                        @Override
                        public Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
                            exceptFilter(Callback<RequestT, Exception, ClientT, ModelT, CallbackT, Boolean> handler) {
                            return exceptHandler(
                                (request, exception, client1, model1, context1) -> {
                                    if (handler.invoke(request, exception, client1, model1, context1)) {
                                        return ProgressEvent.progress(model1, context1);
                                    }
                                    return ProgressEvent.failed(model1, context1, HandlerErrorCode.ServiceException, exception.getMessage());
                                });
                        }

                        @Override
                        public Completed<RequestT, ResponseT, ClientT, ModelT, CallbackT>
                            exceptHandler(Callback<RequestT, Exception, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> handler) {
                            this.exceptHandler = handler;
                            return this;
                        }

                        @Override
                        public ProgressEvent<ModelT, CallbackT>
                            done(Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> callback) {
                            Function<ModelT, RequestT> reqMaker = context.request(callGraph, maker);
                            BiFunction<RequestT, ProxyClient<ClientT>, ResponseT> resMaker = context.response(callGraph, caller);
                            if (waitFor != null) {
                                waitFor = context.wait(callGraph, waitFor);
                            }
                            RequestT req = null;
                            ResponseT res = null;
                            int attempt = 1;
                            do {
                                try {
                                    req = req == null ? reqMaker.apply(model) : req;
                                    res = res == null ? resMaker.apply(req, client) : res;
                                    if (waitFor != null) {
                                        if (waitFor.invoke(req, res, client, model, context)) {
                                            return callback.invoke(req, res, client, model, context);
                                        }
                                        continue;
                                    }
                                    return callback.invoke(req, res, client, model, context);
                                }
                                catch (Exception e) {
                                    //
                                    // Client side exception, mapping this to InvalidRequest at the moment
                                    //
                                    if (e instanceof NonRetryableException) {
                                        return ProgressEvent.failed(model, context, HandlerErrorCode.InvalidRequest,
                                            e.getMessage());
                                    }

                                    if (e instanceof AwsServiceException) {
                                        AwsServiceException sdkException = (AwsServiceException) e;
                                        AwsErrorDetails details = sdkException.awsErrorDetails();
                                        switch (details.sdkHttpResponse().statusCode()) {
                                            case 400:
                                                //
                                                // BadRequest, wrong values in the request
                                                //
                                                return ProgressEvent.failed(model, context, HandlerErrorCode.InvalidRequest,
                                                    details.errorCode() + ": " + details.errorMessage());

                                            case 401:
                                            case 403:
                                            case 511: // Network Auth Required, just in case
                                                //
                                                // Access Denied, AuthN/Z problems
                                                //
                                                return ProgressEvent.failed(model, context, HandlerErrorCode.AccessDenied,
                                                    details.errorCode() + ": " + details.errorMessage());

                                            case 404:
                                            case 410:
                                                //
                                                // Resource that we are trying READ/UPDATE/DELETE is not found
                                                //
                                                return ProgressEvent.failed(model, context, HandlerErrorCode.NotFound,
                                                    details.errorCode() + ": " + details.errorMessage());

                                            case 500:
                                                //
                                                // Often retries help here as well. IMP to remember here that
                                                // there are retries with the SDK Client itself for these. Verify
                                                // what we add extra over the default ones
                                                //
                                            case 503:
                                            case 504:
                                            case 429: // Throttle, TOO many requests
                                                AmazonWebServicesClientProxy.this.logger.log(
                                                    "Retrying " + callGraph + " for error " + details.errorMessage());
                                                continue;
                                        }
                                    }

                                    if (exceptHandler != null) {
                                        ProgressEvent<ModelT, CallbackT> handled = exceptHandler.invoke(req, e, client, model, context);
                                        if (handled.isFailed()) {
                                            return handled;
                                        }
                                    }
                                }
                            } while (waitForNext(delay, attempt++));
                            return null;
                        }

                        @Override
                        public ProgressEvent<ModelT, CallbackT> done(Function<ResponseT, ProgressEvent<ModelT, CallbackT>> func) {
                            return done((request1, response1, client1, model1, context1) -> func.apply(response1));
                        }
                    };
                }
            };
        }

        private boolean waitForNext(Delay delay, int attempt) {
            long waitTime = delay.nextDelay(attempt);
            try {
                delay.unit().wait(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
    }

    private final AWSCredentialsProvider v1CredentialsProvider;

    private final AwsCredentialsProvider v2CredentialsProvider;

    private final LambdaLogger logger;

    public AmazonWebServicesClientProxy(
        final LambdaLogger logger,
        final Credentials credentials) {
        this.logger = logger;

        final BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
        this.v1CredentialsProvider = new AWSStaticCredentialsProvider(basicSessionCredentials);

        final AwsSessionCredentials awsSessionCredentials = AwsSessionCredentials.create(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());
        this.v2CredentialsProvider = StaticCredentialsProvider.create(awsSessionCredentials);
    }

    public <RequestT extends AmazonWebServiceRequest, ResultT extends AmazonWebServiceResult<ResponseMetadata>> ResultT injectCredentialsAndInvoke(
        final RequestT request,
        final Function<RequestT, ResultT> requestFunction) {

        request.setRequestCredentialsProvider(v1CredentialsProvider);

        try {
            return requestFunction.apply(request);
        } catch (final Exception e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        } finally {
            request.setRequestCredentialsProvider(null);
        }
    }

    public <RequestT extends AwsRequest, ResultT extends AwsResponse> ResultT injectCredentialsAndInvokeV2(
            final RequestT request,
            final Function<RequestT, ResultT> requestFunction) {

        final AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                .credentialsProvider(v2CredentialsProvider)
                .build();

        @SuppressWarnings("unchecked")
        final RequestT wrappedRequest = (RequestT)request.toBuilder()
                .overrideConfiguration(overrideConfiguration)
                .build();

        try {
            return requestFunction.apply(wrappedRequest);
        } catch (final Exception e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        }
    }

    public <RequestT extends AwsRequest, ResultT extends AwsResponse> CompletableFuture<ResultT> injectCredentialsAndInvokeV2Async(
            final RequestT request,
            final Function<RequestT, CompletableFuture<ResultT>> requestFunction) {

        final AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                .credentialsProvider(v2CredentialsProvider)
                .build();

        @SuppressWarnings("unchecked")
        final RequestT wrappedRequest = (RequestT)request.toBuilder()
                .overrideConfiguration(overrideConfiguration)
                .build();

        try {
            return requestFunction.apply(wrappedRequest);
        } catch (final Exception e) {
            logger.log(String.format("Failed to execute remote function: {%s}", e.getMessage()));
            throw e;
        }
    }
}
