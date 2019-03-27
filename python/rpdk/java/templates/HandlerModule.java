package {{ package_name }};

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.aws.cfn.injection.AmazonCloudFormationProvider;
import com.aws.cfn.proxy.CallbackAdapter;
import com.aws.cfn.proxy.CloudFormationCallbackAdapter;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class HandlerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AmazonCloudFormation.class).toProvider(AmazonCloudFormationProvider.class);
        bind(new TypeLiteral<CallbackAdapter<{{ pojo_name }}>>() {})
                .to(new TypeLiteral<CloudFormationCallbackAdapter<{{ pojo_name }}>>() {});
    }
}
