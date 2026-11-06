package com.observability.monitor.service.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Immutable snapshot of a customer order.
 */
public record Order(
        String orderId,
        String customerId,
        List<OrderLine> lines,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public Order withStatus(OrderStatus newStatus, Instant changedAt) {
        return new Order(orderId, customerId, lines, totalAmount, newStatus, createdAt, changedAt);
    }
}
