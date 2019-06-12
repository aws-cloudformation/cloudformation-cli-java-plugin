package com.amazonaws.cloudformation.proxy;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This implements the proxying mechanism to inject appropriate scoped credentials
 * into a service call when making AWS Webservice calls.
 *
 * @see CallChain
 * @see ProxyClient
 */
public class AmazonWebServicesClientProxy implements CallChain {

    public <ClientT> ProxyClient<ClientT> newProxy(@Nonnull Supplier<ClientT> client) {
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
                return client.get();
            }
        };
    }

    @Override
    public <ClientT, ModelT, CallbackT extends StdCallbackContext> RequestMaker<ClientT, ModelT, CallbackT>
        initiate(String callGraph, ProxyClient<ClientT> client, ModelT model, CallbackT cxt) {
        Preconditions.checkNotNull(callGraph, "callGraph must be specified");
        Preconditions.checkNotNull(client, "ProxyClient can not be null");
        Preconditions.checkNotNull(model, "Resource Model can not be null");
        Preconditions.checkNotNull(cxt, "cxt can not be null");
        return new CallContext<>(callGraph, client, model, cxt);
    }

    class CallContext<ClientT, ModelT, CallbackT extends StdCallbackContext>
        implements CallChain.RequestMaker<ClientT, ModelT, CallbackT> {

        private final String callGraph;
        private final ProxyClient<ClientT> client;
        private final ModelT model;
        private final CallbackT context;
        //
        // Default delay context and retries for all web service calls when
        // handling errors, throttles and more. The handler can influence this
        // using retry method.
        //
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
                        // default exception handler, reports failure.
                        private Callback<RequestT, Exception, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> defaultHandler =
                            (request, exception, client1, model1, context1) -> {
                                String errMsg;
                                if (exception instanceof AwsServiceException) {
                                    AwsErrorDetails details = ((AwsServiceException)exception).awsErrorDetails();
                                    errMsg = "Code(" + details.errorCode() + "),  "  + details.errorMessage();
                                }
                                else {
                                    errMsg = exception.getMessage();
                                }
                                return ProgressEvent.failed(
                                    model1, context1, HandlerErrorCode.ServiceException, errMsg);
                            };
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
                                    return defaultHandler.invoke(request, exception, client1, model1, context1);
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
                            //
                            // StdCallbackContext memoization wrappers for request, response, and stabilization
                            // lambdas. This ensures that we call demux as necessary.
                            //
                            Function<ModelT, RequestT> reqMaker = context.request(callGraph, maker);
                            BiFunction<RequestT, ProxyClient<ClientT>, ResponseT> resMaker = context.response(callGraph, caller);
                            if (waitFor != null) {
                                waitFor = context.stabilize(callGraph, waitFor);
                            }
                            Callback<RequestT, Exception, ClientT, ModelT, CallbackT, ProgressEvent<ModelT, CallbackT>> exceptHandler =
                                this.exceptHandler != null ? this.exceptHandler : this.defaultHandler;
                            RequestT req = null;
                            ResponseT res = null;
                            int attempt = context.attempts(callGraph);
                            for(;;) {
                                Instant now = Instant.now();
                                wait: try {
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

                                            case 503:
                                                //
                                                // Often retries help here as well. IMP to remember here that
                                                // there are retries with the SDK Client itself for these. Verify
                                                // what we add extra over the default ones
                                                //
                                            case 504:
                                            case 429: // Throttle, TOO many requests
                                                AmazonWebServicesClientProxy.this.logger.log(
                                                    "Retrying " + callGraph + " for error " + details.errorMessage());
                                                break wait;
                                        }
                                    }

                                    ProgressEvent<ModelT, CallbackT> handled = exceptHandler.invoke(req, e, client, model, context);
                                    if (handled.isFailed()) {
                                        return handled;
                                    }
                                }
                                //
                                // The logic to wait is if next delay + 2 * time to run the operation sequence + 100ms
                                // is less than time remaining time to run inside Lambda then we locally wait
                                // else we bail out. Assuming 3 DAYS for a DB to restore, that would be total of
                                // 3 x 24 x 60 x 60 x 1000 ms, fits in 32 bit int.
                                //
                                Instant opTime = Instant.now();
                                long elapsed = ChronoUnit.MILLIS.between(now, opTime);
                                long next = delay.nextDelay(attempt++);
                                context.attempts(callGraph, attempt);
                                if (next < 0) {
                                    return ProgressEvent.failed(model, context,
                                        HandlerErrorCode.ServiceException, "Exceeded attempts to wait");
                                }
                                long remainingTime = getRemainingTimeInMillis();
                                long localWait = delay.unit().toMillis(next) + 2 * elapsed + 100;
                                if (remainingTime > localWait) {
                                    Uninterruptibles.sleepUninterruptibly(next, delay.unit());
                                    continue;
                                }
                                return ProgressEvent.<ModelT, CallbackT>builder()
                                    .status(OperationStatus.IN_PROGRESS)
                                    .callbackContext(context)
                                    .resourceModel(model)
                                    // TODO fix this to be long
                                    .callbackDelaySeconds((int)delay.unit().toSeconds(next))
                                    .build();
                            }
                        }

                        @Override
                        public ProgressEvent<ModelT, CallbackT> done(Function<ResponseT, ProgressEvent<ModelT, CallbackT>> func) {
                            return done((request1, response1, client1, model1, context1) -> func.apply(response1));
                        }
                    };
                }
            };
        }

    }

    private final AWSCredentialsProvider v1CredentialsProvider;
    private final AwsCredentialsProvider v2CredentialsProvider;
    private final LambdaLogger logger;
    private final Supplier<Integer> remainingTimeInMillis;

    public AmazonWebServicesClientProxy(
        final LambdaLogger logger,
        final Credentials credentials,
        final Supplier<Integer> remainingTimeToExecute) {
        this.logger = logger;
        this.remainingTimeInMillis = remainingTimeToExecute;

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

    public final long getRemainingTimeInMillis() {
        return (long) remainingTimeInMillis.get();
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
