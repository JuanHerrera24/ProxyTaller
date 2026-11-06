package com.observability.monitor.service.order;

import java.math.BigDecimal;

public record OrderLine(String sku, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

    public static OrderLine of(String sku, int quantity, BigDecimal unitPrice) {
        return new OrderLine(sku, quantity, unitPrice, unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }
}
