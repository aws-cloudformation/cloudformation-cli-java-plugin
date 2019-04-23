package com.aws.cfn.oasis.model.iteration;

import com.aws.cfn.oasis.model.Template;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents the type of iteration that will be applied by updating
 * the stack to the given template
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ApplyTemplateIteration implements Iteration {
    @NonNull private final Template template;
}
