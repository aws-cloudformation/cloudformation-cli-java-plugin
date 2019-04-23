package com.aws.cfn.oasis.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * The bundle of template context to pass along to the handler.  It includes
 * the previous template and the template of the final target state
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Templates {
    @NonNull private final Template initialTemplate;
    @NonNull private final Template finalTemplate;
}
