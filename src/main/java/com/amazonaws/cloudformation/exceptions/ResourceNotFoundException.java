package com.amazonaws.cloudformation.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -1646136434112354328L;

    public ResourceNotFoundException(final Throwable cause) {
        super(null, cause);
    }

    public ResourceNotFoundException(final String resourceTypeName,
                                     final String resourceIdentifier) {
        this(resourceTypeName, resourceIdentifier, null);
    }

    public ResourceNotFoundException(final String resourceTypeName,
                                     final String resourceIdentifier,
                                     final Throwable cause) {
        super(String.format("Resource of type '%s' with identifier '%s' was not found.",
            resourceTypeName, resourceIdentifier), cause);
    }
}
