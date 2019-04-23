package com.aws.cfn.oasis.model.iteration;

/**
 * An iteration represents one step along the process of
 * applying a stack operation.  It will most commonly be applied
 * by performing an update from the existing template to a new one
 * but can also be another type of command like renaming a logical id
 */
public interface Iteration {
}
