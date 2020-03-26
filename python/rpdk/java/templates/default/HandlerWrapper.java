// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.amazonaws.AmazonServiceException;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.LambdaWrapper;
import software.amazon.cloudformation.loggers.LambdaLogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallbackAdapter;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.HandlerRequest;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.RequestContext;
import software.amazon.cloudformation.proxy.RequestData;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.ResourceHandlerTestPayload;
import software.amazon.cloudformation.resource.SchemaValidator;
import software.amazon.cloudformation.resource.Serializer;
import software.amazon.cloudformation.scheduler.CloudWatchScheduler;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;


public final class HandlerWrapper extends LambdaWrapper<{{ pojo_name }}, CallbackContext> {

    private final Configuration configuration = new Configuration();
    private JSONObject resourceSchema;
    private final Map<Action, BaseHandler<CallbackContext>> handlers = new HashMap<>();
    private final static TypeReference<HandlerRequest<{{ pojo_name }}, CallbackContext>> REQUEST_REFERENCE =
        new TypeReference<HandlerRequest<{{ pojo_name }}, CallbackContext>>() {};
    private final static TypeReference<{{ pojo_name }}> TYPE_REFERENCE =
        new TypeReference<{{ pojo_name }}>() {};
    private final static TypeReference<ResourceHandlerTestPayload<{{ pojo_name }}, CallbackContext>> TEST_ENTRY_TYPE_REFERENCE =
        new TypeReference<ResourceHandlerTestPayload<{{ pojo_name }}, CallbackContext>>() {};


    public HandlerWrapper() {
        initialiseHandlers();
    }

    private void initialiseHandlers() {
{% for op in operations %}
        handlers.put(Action.{{ op|upper }}, new {{ op|uppercase_first_letter }}Handler());
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

        loggerProxy.log(String.format("[%s] invoking handler...", actionName));
        final ProgressEvent<{{ pojo_name }}, CallbackContext> result = handler.handleRequest(proxy, request, callbackContext, loggerProxy);
        loggerProxy.log(String.format("[%s] handler invoked", actionName));
        return result;
    }

    public void testEntrypoint(
            final InputStream inputStream,
            final OutputStream outputStream,
            final Context context) throws IOException {

        this.loggerProxy = new LoggerProxy();
        this.loggerProxy.addLogPublisher(new LambdaLogPublisher(context.getLogger()));

        ProgressEvent<{{ pojo_name }}, CallbackContext> response = ProgressEvent.failed(null, null, HandlerErrorCode.InternalFailure, "Uninitialized");
        try {
            final String input = IOUtils.toString(inputStream, "UTF-8");
            final ResourceHandlerTestPayload<{{ pojo_name }}, CallbackContext> payload =
                this.serializer.deserialize(
                    input,
                    TEST_ENTRY_TYPE_REFERENCE);

            final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
                loggerProxy, payload.getCredentials(), () -> (long) context.getRemainingTimeInMillis());

            response = invokeHandler(proxy, payload.getRequest(), payload.getAction(), payload.getCallbackContext());
        } catch (final BaseHandlerException e) {
            response = ProgressEvent.defaultFailureHandler(e, e.getErrorCode());
        } catch (final AmazonServiceException e) {
            response = ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.GeneralServiceException);
        } catch (final Throwable e) {
            e.printStackTrace();
            response = ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InternalFailure);
        } finally {
            final String output = this.serializer.serialize(response);
            outputStream.write(output.getBytes(Charset.forName("UTF-8")));
            outputStream.close();
        }
    }

    @Override
    public JSONObject provideResourceSchemaJSONObject() {
        if (resourceSchema == null) {
            resourceSchema = this.configuration.resourceSchemaJSONObject();
        }
        return resourceSchema;
    }

    @Override
    public Map<String, String> provideResourceDefinedTags(final {{ pojo_name}} resourceModel) {
        return this.configuration.resourceDefinedTags(resourceModel);
    }

    @Override
    protected ResourceHandlerRequest<{{ pojo_name }}> transform(final HandlerRequest<{{ pojo_name }}, CallbackContext> request) throws IOException {
        final RequestData<{{ pojo_name }}> requestData = request.getRequestData();

        return ResourceHandlerRequest.<{{ pojo_name }}>builder()
            .clientRequestToken(request.getBearerToken())
            .desiredResourceState(requestData.getResourceProperties())
            .previousResourceState(requestData.getPreviousResourceProperties())
            .desiredResourceTags(getDesiredResourceTags(request))
            .systemTags(request.getRequestData().getSystemTags())
            .awsAccountId(request.getAwsAccountId())
            .logicalResourceIdentifier(request.getRequestData().getLogicalResourceId())
            .nextToken(request.getNextToken())
            .region(request.getRegion())
            .build();
    }

    @Override
    protected TypeReference<HandlerRequest<{{ pojo_name }}, CallbackContext>> getTypeReference() {
        return REQUEST_REFERENCE;
    }

    @Override
    protected TypeReference<{{ pojo_name }}> getModelTypeReference() {
        return TYPE_REFERENCE;
    }
}
