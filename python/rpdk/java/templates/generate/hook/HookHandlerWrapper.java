// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.amazonaws.AmazonServiceException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.cloudformation.HookInvocationPoint;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.{{ wrapper_parent }};
import software.amazon.cloudformation.loggers.LambdaLogPublisher;
import software.amazon.cloudformation.metrics.MetricsPublisher;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.hook.HookContext;
import software.amazon.cloudformation.proxy.hook.HookInvocationRequest;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.RequestContext;
import software.amazon.cloudformation.proxy.hook.HookHandlerRequest;
import software.amazon.cloudformation.proxy.hook.HookHandlerTestPayload;
import software.amazon.cloudformation.proxy.hook.targetmodel.HookTargetModel;
import software.amazon.cloudformation.resource.Serializer;
import software.amazon.cloudformation.scheduler.CloudWatchScheduler;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public final class {{ "HookHandlerWrapper" if wrapper_parent == "HookLambdaWrapper" else "HookHandlerWrapperExecutable" }} extends {{ wrapper_parent }}<HookTargetModel, CallbackContext, TypeConfigurationModel> {

    private final Configuration configuration = new Configuration();
    private JSONObject hookSchema;
    private final Map<HookInvocationPoint, BaseHookHandler<CallbackContext, TypeConfigurationModel>> handlers = new HashMap<>();
    private final static TypeReference<HookInvocationRequest<TypeConfigurationModel, CallbackContext>> REQUEST_REFERENCE =
        new TypeReference<HookInvocationRequest<TypeConfigurationModel, CallbackContext>>() {};
    private final static TypeReference<TypeConfigurationModel> TYPE_REFERENCE =
        new TypeReference<TypeConfigurationModel>() {};
    private final static TypeReference<HookHandlerTestPayload<TypeConfigurationModel, CallbackContext>> TEST_ENTRY_TYPE_REFERENCE =
        new TypeReference<HookHandlerTestPayload<TypeConfigurationModel, CallbackContext>>() {};

    public {{ "HookHandlerWrapper" if wrapper_parent == "HookLambdaWrapper" else "HookHandlerWrapperExecutable" }}() {
        initialiseHandlers();
    }

    private void initialiseHandlers() {
{% for op in operations %}
        handlers.put(HookInvocationPoint.fromShortName("{{ op }}"), new {{ op|uppercase_first_letter }}HookHandler());
{% endfor %}
    }

    @Override
    public ProgressEvent<HookTargetModel, CallbackContext> invokeHandler(
                final AmazonWebServicesClientProxy proxy,
                final HookHandlerRequest request,
                final HookInvocationPoint invocationPoint,
                final CallbackContext callbackContext,
                final TypeConfigurationModel typeConfiguration) {

        final String invocationPointName = (invocationPoint == null) ? "<null>" : invocationPoint.toString(); // paranoia
        if (!handlers.containsKey(invocationPoint))
            throw new RuntimeException("Unknown invocationPoint " + invocationPointName);

        final BaseHookHandler<CallbackContext, TypeConfigurationModel> handler = handlers.get(invocationPoint);

        loggerProxy.log(String.format("[%s] invoking handler...", invocationPointName));
        final ProgressEvent<HookTargetModel, CallbackContext> result = handler.handleRequest(proxy, request, callbackContext, loggerProxy, typeConfiguration);
        loggerProxy.log(String.format("[%s] handler invoked", invocationPointName));
        return result;
    }

    {% if wrapper_parent == "HookLambdaWrapper" -%}


    public void testEntrypoint(
            final InputStream inputStream,
            final OutputStream outputStream,
            final Context context) throws IOException {

        this.loggerProxy = new LoggerProxy();
        this.loggerProxy.addLogPublisher(new LambdaLogPublisher(context.getLogger()));

        ProgressEvent<HookTargetModel, CallbackContext> response = ProgressEvent.failed(null, null, HandlerErrorCode.InternalFailure, "Uninitialized");
        try {
            final String input = IOUtils.toString(inputStream, "UTF-8");
            final HookHandlerTestPayload<TypeConfigurationModel, CallbackContext> payload =
                this.serializer.deserialize(
                    input,
                    TEST_ENTRY_TYPE_REFERENCE);

            final AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
                loggerProxy, payload.getCredentials(), () -> (long) context.getRemainingTimeInMillis());

            response = invokeHandler(proxy, payload.getRequest(), payload.getActionInvocationPoint(), payload.getCallbackContext(), payload.getTypeConfiguration());
        } catch (final BaseHandlerException e) {
            response = ProgressEvent.defaultFailureHandler(e, e.getErrorCode());
        } catch (final AmazonServiceException | AwsServiceException e) {
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
    {% else %}
    public static void main(final String[] args) throws IOException {
        if (args.length != 1){
            System.exit(1);
        }
        final String outputFile = (UUID.randomUUID().toString() + ".txt");
        try(final FileOutputStream output = new FileOutputStream(outputFile)){
            try(final InputStream input = IOUtils.toInputStream(args[0],"UTF-8")){
                new HookHandlerWrapperExecutable().handleRequest(input, output);
                output.flush();
            }
        }
        System.out.println("__CFN_HOOK_START_RESPONSE__");
        readFileToSystemOut(outputFile);
        System.out.println("__CFN_HOOK_END_RESPONSE__");
    }

    private static void readFileToSystemOut(final String fileName) throws IOException {
        //Create object of FileReader
        final FileReader inputFile = new FileReader(fileName);
        try(final BufferedReader bufferReader = new BufferedReader(inputFile)) {
            String line;
            while ((line = bufferReader.readLine()) != null)   {
                System.out.println(line);
            }
        }
    }
    {%- endif %}

    @Override
    public JSONObject provideHookSchemaJSONObject() {
        if (hookSchema == null) {
            hookSchema = this.configuration.hookSchemaJSONObject();
        }
        return hookSchema;
    }

    @Override
    protected HookHandlerRequest transform(final HookInvocationRequest<TypeConfigurationModel, CallbackContext> request) throws IOException {
        return HookHandlerRequest.builder()
            .clientRequestToken(request.getClientRequestToken())
            .hookContext(HookContext
                    .builder()
                    .awsAccountId(request.getAwsAccountId())
                    .stackId(request.getStackId())
                    .changeSetId(request.getChangeSetId())
                    .hookTypeName(request.getHookTypeName())
                    .hookTypeVersion(request.getHookTypeVersion())
                    .invocationPoint(request.getActionInvocationPoint())
                    .targetName(request.getRequestData().getTargetName())
                    .targetType(request.getRequestData().getTargetType())
                    .targetLogicalId(request.getRequestData().getTargetLogicalId())
                    .targetModel(HookTargetModel.of(request.getRequestData().getTargetModel()))
                    .build())
            .build();
    }

    @Override
    protected TypeReference<HookInvocationRequest<TypeConfigurationModel, CallbackContext>> getTypeReference() {
        return REQUEST_REFERENCE;
    }

    @Override
    protected TypeReference<TypeConfigurationModel> getModelTypeReference() {
        return TYPE_REFERENCE;
    }
}
