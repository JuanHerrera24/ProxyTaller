package com.observability.monitor.service.inventory;

import java.math.BigDecimal;

/**
 * Immutable snapshot of a catalog item and its stock levels.
 */
public record Product(
        String sku,
        String name,
        String category,
        BigDecimal unitPrice,
        int availableQuantity,
        int reservedQuantity) {

    public Product withStock(int available, int reserved) {
        return new Product(sku, name, category, unitPrice, available, reserved);
    }
}
