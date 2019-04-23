// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.aws.cfn.oasis.model.ProgressEvent;
import com.aws.cfn.oasis.model.IterationProviderHandlerRequest;
import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseIterationProviderHandler<CallbackT> {
    public abstract ProgressEvent handleRequest(
            @Nonnull final AmazonWebServicesClientProxy proxy,
            @Nonnull final IterationProviderHandlerRequest request,
            @Nullable final CallbackT callbackContext,
            @Nonnull final Logger logger
    );
}
