package com.observability.monitor.service.order;

import com.observability.monitor.exception.BusinessRuleException;
import com.observability.monitor.exception.ResourceNotFoundException;
import com.observability.monitor.service.support.AbstractManagedService;
import com.observability.monitor.service.support.LatencySimulator;
import com.observability.monitor.service.support.OperationParameters;
import com.observability.monitor.service.support.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles the order lifecycle: creation, lookup, status transitions and cancellation.
 */
@Service
public class OrderService extends AbstractManagedService {

    public static final String SERVICE_ID = "orders";

    public static final String CREATE_ORDER = "create-order";
    public static final String GET_ORDER = "get-order";
    public static final String LIST_ORDERS = "list-orders";
    public static final String UPDATE_STATUS = "update-status";
    public static final String CANCEL_ORDER = "cancel-order";

    private final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public OrderService() {
        this(new LatencySimulator(30, 160), Clock.systemUTC());
    }

    public OrderService(LatencySimulator latency, Clock clock) {
        super(latency);
        this.clock = clock;

        register(CREATE_ORDER, this::createOrder);
        register(GET_ORDER, this::getOrder);
        register(LIST_ORDERS, this::listOrders);
        register(UPDATE_STATUS, this::updateStatus);
        register(CANCEL_ORDER, this::cancelOrder);
    }

    @Override
    public String serviceId() {
        return SERVICE_ID;
    }

    @Override
    public String displayName() {
        return "Order Service";
    }

    public List<String> orderIds() {
        return List.copyOf(orders.keySet());
    }

    public Optional<Order> findOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    private ServiceResponse createOrder(OperationParameters params) {
        String customerId = params.requireText("customerId");
        List<OrderLine> lines = params.requireObjectList("items").stream()
                .map(item -> OrderLine.of(
                        item.requireText("sku"),
                        item.requirePositiveInt("quantity"),
                        item.requirePositiveAmount("unitPrice")))
                .toList();

        BigDecimal total = lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant now = clock.instant();
        Order order = new Order(nextOrderId(), customerId, lines, total, OrderStatus.CREATED, now, now);
        orders.put(order.orderId(), order);

        return ServiceResponse.of("Order created", order);
    }

    private ServiceResponse getOrder(OperationParameters params) {
        return ServiceResponse.of("Order retrieved", requireOrder(params.requireText("orderId")));
    }

    private ServiceResponse listOrders(OperationParameters params) {
        Optional<OrderStatus> statusFilter = params.optionalText("status").map(OrderStatus::parse);
        Optional<String> customerFilter = params.optionalText("customerId");

        List<Order> result = orders.values().stream()
                .filter(order -> statusFilter.map(status -> order.status() == status).orElse(true))
                .filter(order -> customerFilter.map(customer -> order.customerId().equals(customer)).orElse(true))
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .toList();

        return ServiceResponse.of("%d orders found".formatted(result.size()), result);
    }

    private ServiceResponse updateStatus(OperationParameters params) {
        String orderId = params.requireText("orderId");
        OrderStatus target = OrderStatus.parse(params.requireText("status"));
        Order updated = transition(orderId, target);
        return ServiceResponse.of("Order moved to %s".formatted(target), updated);
    }

    private ServiceResponse cancelOrder(OperationParameters params) {
        Order cancelled = transition(params.requireText("orderId"), OrderStatus.CANCELLED);
        return ServiceResponse.of("Order cancelled", cancelled);
    }

    private Order transition(String orderId, OrderStatus target) {
        Order updated = orders.computeIfPresent(orderId, (id, current) -> {
            if (!current.status().canTransitionTo(target)) {
                throw new BusinessRuleException("Order '%s' cannot move from %s to %s"
                        .formatted(id, current.status(), target));
            }
            return current.withStatus(target, clock.instant());
        });
        if (updated == null) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return updated;
    }

    private Order requireOrder(String orderId) {
        return findOrder(orderId).orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private static String nextOrderId() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
