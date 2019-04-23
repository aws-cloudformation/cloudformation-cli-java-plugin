package com.aws.cfn.oasis.model.iteration;

import com.aws.cfn.oasis.model.entity.LogicalId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Applying this iteration will involve CloudFormation changing the logical ids
 * of the specified resources both within the template (and all references to them)
 * as well as within the data stores
 */
@Value
public class RenameLogicalIdIteration implements Iteration {
    @NonNull private final Map<LogicalId, LogicalId> logicalIdMappings;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<LogicalId, LogicalId> logicalIdMappings = new HashMap<>();

        public Builder logicalIdMapping(@Nonnull final LogicalId oldLogicalId, @Nonnull final LogicalId newLogicalId) {
            Objects.requireNonNull(oldLogicalId);
            Objects.requireNonNull(newLogicalId);

            if (oldLogicalId.equals(newLogicalId)) {
                throw new IllegalArgumentException(
                        String.format("Cannot map logical id [%s] to itself", oldLogicalId)
                );
            }

            logicalIdMappings.put(oldLogicalId, newLogicalId);

            return this;
        }

        public RenameLogicalIdIteration build() {
            return new RenameLogicalIdIteration(logicalIdMappings);
        }
    }
}
