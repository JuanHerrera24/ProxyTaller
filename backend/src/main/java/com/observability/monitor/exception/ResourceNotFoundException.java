package com.observability.monitor.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String identifier) {
        super("%s '%s' was not found".formatted(resource, identifier));
    }
}
