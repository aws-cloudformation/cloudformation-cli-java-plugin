package com.amazonaws.cloudformation.proxy;

import com.amazonaws.cloudformation.proxy.handler.Model;
import com.amazonaws.cloudformation.proxy.service.CreateRequest;
import com.amazonaws.cloudformation.proxy.service.ServiceClient;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.joda.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class CallChainTest {

    static final AwsServiceException.Builder builder = Mockito.mock(AwsServiceException.Builder.class);

    private final Credentials credentials =
       new Credentials("accessKeyId", "secretKey", "seesionToken");
    private final ServiceClient client = new ServiceClient();

    @Test
    public void happyCase() {
        AmazonWebServicesClientProxy proxy = new AmazonWebServicesClientProxy(
            Mockito.mock(LambdaLogger.class),
            credentials,
            () -> (int)Duration.standardMinutes(10).getStandardSeconds()
        );
        Model model = Model.builder().repoName("repo").build();
        StdCallbackContext context = new StdCallbackContext();
        ProxyClient<ServiceClient> client = proxy.newProxy(() -> this.client);
        ProgressEvent<Model, StdCallbackContext> event =
            proxy.initiate("client:createRepository", client, model, context)
            .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
            .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
            .done(r -> ProgressEvent.success(model, context));

        Assertions.assertEquals(OperationStatus.SUCCESS, event.getStatus());

        // replay, should get the same result.
        event =
            proxy.initiate("client:createRepository", client, model, context)
            .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
            .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
            .done(r -> ProgressEvent.success(model, context));

        Assertions.assertEquals(OperationStatus.SUCCESS, event.getStatus());

        // Now a separate request
        StdCallbackContext newContext = new StdCallbackContext();
        event =
            proxy.initiate("client:createRepository", client, model, newContext)
                .request((m) -> new CreateRequest.Builder().repoName(m.getRepoName()).build())
                .call((r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createRepository))
                .done(r -> ProgressEvent.success(model, context));

        Assertions.assertEquals(OperationStatus.FAILED, event.getStatus());
        Assertions.assertTrue(event.getMessage().contains("AlreadyExists"));

    }
}
