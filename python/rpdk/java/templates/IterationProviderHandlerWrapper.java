// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.aws.cfn.Action;
import com.aws.cfn.oasis.LambdaWrapper;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.oasis.model.ProgressEvent;
import com.aws.cfn.oasis.model.IterationProviderHandlerRequest;
import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.oasis.model.IterationProviderWrapperRequest;
import com.aws.cfn.proxy.LoggerProxy;
import com.aws.cfn.proxy.RequestContext;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Serializer;
import com.aws.cfn.oasis.scheduler.CloudWatchScheduler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;
import com.google.inject.Key;
import com.aws.cfn.oasis.model.iteration.Iteration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class IterationProviderHandlerWrapper extends LambdaWrapper<{{ pojo_name }}, CallbackContext> {

    private final Configuration configuration = new Configuration();
    private final Map<Action, BaseIterationProviderHandler> handlers = new HashMap<>();

    public IterationProviderHandlerWrapper() {
    }

    @Inject
    public IterationProviderHandlerWrapper(final CallbackAdapter<Collection<Iteration>> callbackAdapter,
                                           final MetricsPublisher metricsPublisher,
                                           final CloudWatchScheduler scheduler,
                                           final SchemaValidator validator,
                                           final Serializer serializer,
                                           final TypeReference<IterationProviderWrapperRequest<{{ pojo_name }}, CallbackContext>> typeReference) {
        super(callbackAdapter, metricsPublisher, scheduler, validator, serializer, typeReference);
    }

    @Override
    public ProgressEvent<CallbackContext> invokeHandler(final AmazonWebServicesClientProxy proxy,
                                                        final IterationProviderHandlerRequest<{{ pojo_name }}> request,
                                                        final Action action,
                                                        final CallbackContext callbackContext) {

        final String actionName = (action == null) ? "<null>" : action.toString(); // paranoia
        final LoggerProxy loggerProxy = new LoggerProxy(this.logger);
        final BaseIterationProviderHandler handler = new GenerateHandler();

        return handler.handleRequest(proxy, request, callbackContext, loggerProxy);
    }

    @Override
    public InputStream provideSchema() {
        return this.configuration.schema();
    }

    @Override
    protected CallbackAdapter<Collection<Iteration>> getCallbackAdapter() {
        final Injector injector = Guice.createInjector(new HandlerModule());
        return injector.getInstance(Key.get(new TypeLiteral<CallbackAdapter<Collection<Iteration>>>() {}));
    }

    @Override
    protected TypeReference<IterationProviderWrapperRequest<{{ pojo_name }}, CallbackContext>> getInputTypeReference() {
        return new TypeReference<IterationProviderWrapperRequest<{{ pojo_name }}, CallbackContext>>() {};
    }
}
