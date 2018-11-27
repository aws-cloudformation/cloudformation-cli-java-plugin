package com.aws.cfn;

import com.aws.rpdk.HandlerRequest;
import com.aws.rpdk.ProgressEvent;
import com.aws.rpdk.RequestContext;
import com.aws.rpdk.ResourceHandler;
import com.aws.rpdk.ResourceModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * NOTE: This is an example handler which needs to be removed as we integrate the RPDK
 */
public final class GenericHandler implements ResourceHandler<ResourceModel> {

    @Override
    public ProgressEvent handleRequest(final HandlerRequest<ResourceModel> request,
                                       final Action action,
                                       final RequestContext context) {

        final Gson gson = new GsonBuilder().create();
        final JsonObject jsonObject = gson.toJsonTree(request.getResourceModel()).getAsJsonObject();
        final ResourceModel model = gson.fromJson(jsonObject.toString(), ResourceModel.class);

        final String message = String.format("%s %s by %s [%s].",
            action,
            model.getTitle(),
            model.getAuthor(),
            context.getInvocation());

        return new ProgressEvent(ProgressStatus.InProgress, message, null, 5);
    }
}
