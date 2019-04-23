package {{ package_name }};

import com.aws.cfn.oasis.model.IterationProviderHandlerRequest;
import com.aws.cfn.oasis.model.ProgressEvent;
import com.aws.cfn.oasis.model.iteration.ApplyTemplateIteration;
import com.aws.cfn.oasis.model.nextaction.NextActionApplyIterations;
import com.aws.cfn.proxy.AmazonWebServicesClientProxy;
import com.aws.cfn.proxy.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

public class {{ operation }}Handler extends BaseIterationProviderHandler<CallbackContext> {

    @Override
    public ProgressEvent handleRequest(@Nonnull final AmazonWebServicesClientProxy proxy,
                                       @Nonnull final IterationProviderHandlerRequest request,
                                       @Nullable final CallbackContext callbackContext,
                                       @Nonnull final Logger logger) {
        Objects.requireNonNull(proxy);
        Objects.requireNonNull(request);
        Objects.requireNonNull(logger);

        // TODO produce the templates and stuff for demo

        return new ProgressEvent<CallbackContext>(new NextActionApplyIterations<>(Arrays.asList(
        new ApplyTemplateIteration(request.getTemplates().getFinalTemplate())
        )));
    }
}
