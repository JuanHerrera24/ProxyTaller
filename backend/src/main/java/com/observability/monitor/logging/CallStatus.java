package com.observability.monitor.logging;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum CallStatus {
    SUCCESS,
    ERROR;

    /**
     * Case-insensitive lookup that produces a descriptive message for unknown values.
     */
    public static CallStatus parse(String value) {
        try {
            return CallStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            String accepted = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Unknown status '" + value + "'. Accepted values: " + accepted);
        }
    }
}
