// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.amazonaws.cloudformation.Action;
import com.amazonaws.cloudformation.LambdaWrapper;
import com.amazonaws.cloudformation.metrics.MetricsPublisher;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.CallbackAdapter;
import com.amazonaws.cloudformation.proxy.HandlerRequest;
import com.amazonaws.cloudformation.proxy.LoggerProxy;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.RequestContext;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.proxy.ResourceHandlerTestPayload;
import com.amazonaws.cloudformation.resource.SchemaValidator;
import com.amazonaws.cloudformation.resource.Serializer;
import com.amazonaws.cloudformation.scheduler.CloudWatchScheduler;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class HandlerWrapper extends LambdaWrapper<{{ pojo_name }}, CallbackContext> {

    private final Configuration configuration = new Configuration();
    private final Map<Action, BaseHandler<CallbackContext>> handlers = new HashMap<>();

    public HandlerWrapper() {
        initialiseHandlers();
    }

    private void initialiseHandlers() {
{% for op in operations %}
        handlers.put(Action.{{ op|upper }}, new {{ op }}Handler());
{% endfor %}
    }

    @Override
    public ProgressEvent<{{ pojo_name }}, CallbackContext> invokeHandler(
                final AmazonWebServicesClientProxy proxy,
                final ResourceHandlerRequest<{{ pojo_name }}> request,
                final Action action,
                final CallbackContext callbackContext) {

        final String actionName = (action == null) ? "<null>" : action.toString(); // paranoia
        if (!handlers.containsKey(action))
            throw new RuntimeException("Unknown action " + actionName);

        final BaseHandler<CallbackContext> handler = handlers.get(action);

        return handler.handleRequest(proxy, request, callbackContext, loggerProxy);
    }

    public ProgressEvent<{{ pojo_name }}, CallbackContext> testEntrypoint(
            final ResourceHandlerTestPayload<{{ pojo_name }}, CallbackContext> payload,
            final Context context) {
        final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
                context.getLogger(), payload.getCredentials());

        return invokeHandler(proxy, payload.getRequest(), payload.getAction(), payload.getCallbackContext());
    }

    @Override
    public InputStream provideResourceSchema() {
        return this.configuration.resourceSchema();
    }

    @Override
    protected ResourceHandlerRequest<{{ pojo_name }}> transform(final HandlerRequest<{{ pojo_name }}, CallbackContext> request) throws IOException {
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

        return new ResourceHandlerRequest<{{ pojo_name }}>(
            request.getBearerToken(),
            desiredResourceState,
            previousResourceState,
            request.getRequestData().getLogicalResourceId()
        );
    }

    @Override
    protected TypeReference<HandlerRequest<{{ pojo_name }}, CallbackContext>> getTypeReference() {
        return new TypeReference<HandlerRequest<{{ pojo_name }}, CallbackContext>>() {};
    }
}
