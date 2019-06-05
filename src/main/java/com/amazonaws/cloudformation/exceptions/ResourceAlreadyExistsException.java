package com.amazonaws.cloudformation.exceptions;

public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = -1646136434112354328L;

    public ResourceAlreadyExistsException(final Throwable cause) {
        super(null, cause);
    }

    public ResourceAlreadyExistsException(final String resourceTypeName,
                                          final String resourceIdentifier) {
        this(resourceTypeName, resourceIdentifier, null);
    }

    public ResourceAlreadyExistsException(final String resourceTypeName,
                                          final String resourceIdentifier,
                                          final Throwable cause) {
        super(String.format("Resource of type '%s' with identifier '%s' already exists.",
            resourceTypeName, resourceIdentifier), cause);
    }
}
