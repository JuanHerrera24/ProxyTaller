package com.observability.monitor.service.payment;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable snapshot of a payment transaction.
 */
public record Payment(
        String paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String authorizationCode,
        Instant processedAt,
        Instant refundedAt) {

    public Payment refund(Instant at) {
        return new Payment(paymentId, orderId, amount, currency, method, PaymentStatus.REFUNDED, authorizationCode, processedAt, at);
    }

    public enum PaymentStatus {
        APPROVED,
        REFUNDED
    }
}
