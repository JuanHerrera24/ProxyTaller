package com.observability.monitor.service.payment;

import com.observability.monitor.exception.InvalidRequestException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    DIGITAL_WALLET;

    public static PaymentMethod parse(String value) {
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            String accepted = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new InvalidRequestException("Unknown payment method '%s'. Accepted values: %s".formatted(value, accepted));
        }
    }
}
