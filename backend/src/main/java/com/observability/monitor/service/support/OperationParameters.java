package com.observability.monitor.service.support;

import com.observability.monitor.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Typed, validated accessor over the key/value payload received by a service operation.
 */
public final class OperationParameters {

    private final Map<String, Object> values;

    private OperationParameters(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * Builds the accessor from the variadic arguments of {@code execute}. The first argument is expected to be a map.
     */
    @SuppressWarnings("unchecked")
    public static OperationParameters from(Object... params) {
        if (params == null || params.length == 0 || params[0] == null) {
            return new OperationParameters(Collections.emptyMap());
        }
        if (params[0] instanceof Map<?, ?> map) {
            return new OperationParameters((Map<String, Object>) map);
        }
        throw new InvalidRequestException("Operation parameters must be provided as a JSON object");
    }

    public static OperationParameters of(Map<String, Object> values) {
        return new OperationParameters(values == null ? Collections.emptyMap() : values);
    }

    public String requireText(String key) {
        return optionalText(key).orElseThrow(() -> missing(key));
    }

    public Optional<String> optionalText(String key) {
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    public int requirePositiveInt(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw missing(key);
        }
        int number;
        try {
            number = value instanceof Number n ? toExactInt(key, n) : Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Parameter '%s' must be an integer".formatted(key));
        }
        if (number <= 0) {
            throw new InvalidRequestException("Parameter '%s' must be greater than zero".formatted(key));
        }
        return number;
    }

    public BigDecimal requirePositiveAmount(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw missing(key);
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(value.toString().trim());
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Parameter '%s' must be a decimal number".formatted(key));
        }
        if (amount.signum() <= 0) {
            throw new InvalidRequestException("Parameter '%s' must be greater than zero".formatted(key));
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public List<OperationParameters> requireObjectList(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw missing(key);
        }
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw new InvalidRequestException("Parameter '%s' must be a non-empty array".formatted(key));
        }
        return list.stream().map(item -> {
            if (!(item instanceof Map<?, ?>)) {
                throw new InvalidRequestException("Every element of '%s' must be a JSON object".formatted(key));
            }
            return OperationParameters.from(item);
        }).toList();
    }

    private static int toExactInt(String key, Number number) {
        double asDouble = number.doubleValue();
        if (asDouble != Math.rint(asDouble) || asDouble > Integer.MAX_VALUE || asDouble < Integer.MIN_VALUE) {
            throw new InvalidRequestException("Parameter '%s' must be an integer".formatted(key));
        }
        return number.intValue();
    }

    private static InvalidRequestException missing(String key) {
        return new InvalidRequestException("Parameter '%s' is required".formatted(key));
    }
}
