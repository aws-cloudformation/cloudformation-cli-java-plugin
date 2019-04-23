package com.aws.cfn.oasis.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Represents a CloudFormation template
 */
@EqualsAndHashCode
@AllArgsConstructor
public class Template {
    // TODO object model.
    @NonNull private final String templateBody;

    public String toString() {
        return templateBody;
    }
}
