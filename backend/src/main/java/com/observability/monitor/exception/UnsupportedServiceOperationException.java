package com.observability.monitor.exception;

import java.util.Collection;
import java.util.TreeSet;

public class UnsupportedServiceOperationException extends RuntimeException {

    public UnsupportedServiceOperationException(String serviceId, String operation, Collection<String> supported) {
        super("Operation '%s' is not supported by service '%s'. Supported operations: %s"
                .formatted(operation, serviceId, String.join(", ", new TreeSet<>(supported))));
    }
}
