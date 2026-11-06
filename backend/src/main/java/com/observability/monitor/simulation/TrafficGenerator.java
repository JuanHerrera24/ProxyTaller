package com.observability.monitor.simulation;

import com.observability.monitor.service.inventory.InventoryService;
import com.observability.monitor.service.inventory.Product;
import com.observability.monitor.service.order.Order;
import com.observability.monitor.service.order.OrderService;
import com.observability.monitor.service.payment.PaymentMethod;
import com.observability.monitor.service.payment.PaymentService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Produces realistic, randomized requests for every service. The generated traffic intentionally includes a
 * small share of invalid requests (unknown identifiers, oversized reservations) so that the dashboard also
 * reflects client-side errors, in addition to the gateway failures injected by the payment service.
 */
@Component
public class TrafficGenerator {

    private static final double INVALID_REQUEST_PROBABILITY = 0.04;
    private static final String UNKNOWN_SKU = "SKU-0000";

    private final InventoryService inventory;
    private final OrderService orders;
    private final PaymentService payments;
    private final List<Supplier<SimulatedCall>> scenarios;

    public TrafficGenerator(InventoryService inventory, OrderService orders, PaymentService payments) {
        this.inventory = inventory;
        this.orders = orders;
        this.payments = payments;
        this.scenarios = List.of(
                this::listProducts,
                this::checkStock,
                this::reserveStock,
                this::restock,
                this::createOrder,
                this::createOrder,
                this::readOrder,
                this::advanceOrder,
                this::processPayment,
                this::processPayment,
                this::readPayment,
                this::refundPayment);
    }

    public SimulatedCall next() {
        return scenarios.get(random().nextInt(scenarios.size())).get();
    }

    private SimulatedCall listProducts() {
        return call(InventoryService.SERVICE_ID, InventoryService.LIST_PRODUCTS, Map.of());
    }

    private SimulatedCall checkStock() {
        return call(InventoryService.SERVICE_ID, InventoryService.CHECK_STOCK,
                Map.of("sku", randomSku(), "quantity", random().nextInt(1, 20)));
    }

    private SimulatedCall reserveStock() {
        int quantity = chance(INVALID_REQUEST_PROBABILITY) ? 10_000 : random().nextInt(1, 6);
        return call(InventoryService.SERVICE_ID, InventoryService.RESERVE_STOCK,
                Map.of("sku", randomSku(), "quantity", quantity));
    }

    private SimulatedCall restock() {
        return call(InventoryService.SERVICE_ID, InventoryService.RESTOCK,
                Map.of("sku", randomSku(), "quantity", random().nextInt(10, 60)));
    }

    private SimulatedCall createOrder() {
        List<String> skus = inventory.skus();
        int lineCount = random().nextInt(1, 4);
        List<Map<String, Object>> items = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            String sku = skus.get(random().nextInt(skus.size()));
            BigDecimal unitPrice = inventory.findProduct(sku).map(Product::unitPrice).orElse(BigDecimal.TEN);
            items.add(Map.of("sku", sku, "quantity", random().nextInt(1, 4), "unitPrice", unitPrice));
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("customerId", "CUST-%04d".formatted(random().nextInt(1, 500)));
        params.put("items", items);
        return call(OrderService.SERVICE_ID, OrderService.CREATE_ORDER, params);
    }

    private SimulatedCall readOrder() {
        List<String> ids = orders.orderIds();
        if (ids.isEmpty()) {
            return call(OrderService.SERVICE_ID, OrderService.LIST_ORDERS, Map.of());
        }
        String orderId = chance(INVALID_REQUEST_PROBABILITY) ? "ORD-UNKNOWN" : pick(ids);
        return call(OrderService.SERVICE_ID, OrderService.GET_ORDER, Map.of("orderId", orderId));
    }

    private SimulatedCall advanceOrder() {
        List<String> ids = orders.orderIds();
        if (ids.isEmpty()) {
            return createOrder();
        }
        String orderId = pick(ids);
        String nextStatus = orders.findOrder(orderId)
                .flatMap(order -> order.status().allowedTransitions().stream().findFirst())
                .map(Enum::name)
                .orElse("CONFIRMED");
        return call(OrderService.SERVICE_ID, OrderService.UPDATE_STATUS, Map.of("orderId", orderId, "status", nextStatus));
    }

    private SimulatedCall processPayment() {
        List<String> ids = orders.orderIds();
        String orderId = ids.isEmpty() ? "ORD-%08d".formatted(random().nextInt(100_000_000)) : pick(ids);
        BigDecimal amount = orders.findOrder(orderId)
                .map(Order::totalAmount)
                .orElseGet(() -> BigDecimal.valueOf(random().nextInt(1_000, 50_000), 2));
        PaymentMethod method = PaymentMethod.values()[random().nextInt(PaymentMethod.values().length)];
        return call(PaymentService.SERVICE_ID, PaymentService.PROCESS_PAYMENT,
                Map.of("orderId", orderId, "amount", amount, "currency", "USD", "method", method.name()));
    }

    private SimulatedCall readPayment() {
        List<String> ids = payments.paymentIds();
        if (ids.isEmpty()) {
            return call(PaymentService.SERVICE_ID, PaymentService.LIST_PAYMENTS, Map.of());
        }
        return call(PaymentService.SERVICE_ID, PaymentService.GET_PAYMENT, Map.of("paymentId", pick(ids)));
    }

    private SimulatedCall refundPayment() {
        List<String> ids = payments.refundablePaymentIds();
        if (ids.isEmpty()) {
            return processPayment();
        }
        return call(PaymentService.SERVICE_ID, PaymentService.REFUND_PAYMENT, Map.of("paymentId", pick(ids)));
    }

    private String randomSku() {
        return chance(INVALID_REQUEST_PROBABILITY) ? UNKNOWN_SKU : pick(inventory.skus());
    }

    private static SimulatedCall call(String serviceId, String operation, Map<String, Object> params) {
        return new SimulatedCall(serviceId, operation, params);
    }

    private static <T> T pick(List<T> values) {
        return values.get(random().nextInt(values.size()));
    }

    private static boolean chance(double probability) {
        return random().nextDouble() < probability;
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }
}
