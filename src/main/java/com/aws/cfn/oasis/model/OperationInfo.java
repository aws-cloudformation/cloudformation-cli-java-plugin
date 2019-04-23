package com.aws.cfn.oasis.model;

import com.amazonaws.regions.Region;
import com.aws.cfn.Action;
import com.aws.cfn.oasis.model.entity.AccountId;
import com.aws.cfn.oasis.model.entity.StackId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;

/**
 * A collection of metadata associated with the current state of the
 * overall stack operation
 */
@Value
public class OperationInfo {
    @NonNull private final AccountId accountId;
    @NonNull private final Action action;
    @NonNull private final StackId stackId;
    @NonNull private final Region region;
    @NonNull private final String stage;
    private final boolean isRollback;
}
