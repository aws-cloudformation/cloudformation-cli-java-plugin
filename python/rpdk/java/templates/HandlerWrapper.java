// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.aws.cfn.Action;
import com.aws.cfn.LambdaWrapper;
import com.aws.cfn.metrics.MetricsPublisher;
import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.HandlerRequest;
import com.aws.cfn.proxy.LoggerProxy;
import com.aws.cfn.proxy.ProgressEvent;
import com.aws.cfn.proxy.RequestContext;
import com.aws.cfn.proxy.ResourceHandlerRequest;
import com.aws.cfn.resource.SchemaValidator;
import com.aws.cfn.resource.Serializer;
import com.aws.cfn.scheduler.CloudWatchScheduler;
import com.google.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class HandlerWrapper extends LambdaWrapper<{{ pojo_name }}, CallbackContext> {

    private final Configuration configuration = new Configuration();
    private final Map<Action, BaseHandler> handlers = new HashMap<>();

    public HandlerWrapper() {
        initialiseHandlers();
    }

    @Inject
    public HandlerWrapper(CallbackAdapter callbackAdapter,
                          MetricsPublisher metricsPublisher,
                          CloudWatchScheduler scheduler,
                          SchemaValidator validator,
                          Serializer serializer) {
        super(callbackAdapter, metricsPublisher, scheduler, validator, serializer);
        initialiseHandlers();
        typeReference = new TypeReference<HandlerRequest<{{ pojo_name }}, CallbackContext>>() {};
    }

    private void initialiseHandlers() {
{% for op in operations %}
        handlers.put(Action.{{ op|upper }}, new {{ op }}Handler());
{% endfor %}
    }

    @Override
    public ProgressEvent invokeHandler(final AmazonWebServicesClientProxy proxy,
                                       final ResourceHandlerRequest<{{ pojo_name }}> request,
                                       final Action action,
                                       final CallbackContext callbackContext) {

        final String actionName = (action == null) ? "<null>" : action.toString(); // paranoia
        if (!handlers.containsKey(action))
            throw new RuntimeException("Unknown action " + actionName);

        final LoggerProxy loggerProxy = new LoggerProxy(this.logger);

        final BaseHandler handler = handlers.get(action);

        return handler.handleRequest(proxy, request, callbackContext, loggerProxy);
    }

    @Override
    public InputStream provideResourceSchema() {
        return this.configuration.resourceSchema();
    }

    @Override
    protected ResourceHandlerRequest<{{ pojo_name }}> transform(final HandlerRequest request) throws IOException {
        final {{ pojo_name }} desiredResourceState;
        final {{ pojo_name }} previousResourceState;

        if (request != null &&
            request.getRequestData() != null &&
            request.getRequestData().getResourceProperties() != null) {
            desiredResourceState = request.getRequestData().getResourceProperties();
        } else {
            desiredResourceState = null;
        }

        if (request != null &&
            request.getRequestData() != null &&
            request.getRequestData().getPreviousResourceProperties() != null) {
            previousResourceState = request.getRequestData().getPreviousResourceProperties();
        } else {
            previousResourceState = null;
        }

        return new ResourceHandlerRequest<>(
            request.getAwsAccountId(),
            request.getNextToken(),
            request.getRegion(),
            request.getResourceType(),
            request.getResourceTypeVersion(),
            desiredResourceState,
            previousResourceState
        );
    }
}
