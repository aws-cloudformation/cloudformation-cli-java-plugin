package {{ package_name }};

import com.aws.cfn.injection.CloudFormationProvider;
import com.aws.cfn.oasis.model.iteration.Iteration;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.CloudFormationCallbackAdapter;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;

import java.util.Collection;

public class HandlerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CloudFormationAsyncClient.class).toProvider(CloudFormationProvider.class);
        bind(new TypeLiteral<CallbackAdapter<Collection<Iteration>>>() {})
                .to(new TypeLiteral<CloudFormationCallbackAdapter<Collection<Iteration>>>() {});
    }
}
