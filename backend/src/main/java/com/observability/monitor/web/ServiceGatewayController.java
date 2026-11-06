package com.observability.monitor.web;

import com.observability.monitor.proxy.ProxyExecution;
import com.observability.monitor.registry.ServiceRegistry;
import com.observability.monitor.service.inventory.InventoryService;
import com.observability.monitor.service.order.OrderService;
import com.observability.monitor.service.payment.PaymentService;
import com.observability.monitor.service.support.ServiceResponse;
import com.observability.monitor.web.dto.ServiceCallResponse;
import com.observability.monitor.web.dto.ServiceDescriptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Public entry point to the microservices. Every request is routed through the service's logging proxy.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceGatewayController {

    static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ServiceRegistry registry;

    public ServiceGatewayController(ServiceRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<ServiceDescriptor> listServices() {
        return registry.proxies().stream()
                .map(proxy -> new ServiceDescriptor(
                        proxy.serviceId(),
                        proxy.displayName(),
                        "/api/services/" + proxy.serviceId() + "/{operation}",
                        List.copyOf(proxy.supportedOperations())))
                .toList();
    }

    @PostMapping("/inventory/{operation}")
    public ResponseEntity<ServiceCallResponse> inventory(@PathVariable String operation,
                                                         @RequestBody(required = false) Map<String, Object> body) {
        return dispatch(InventoryService.SERVICE_ID, operation, body);
    }

    @PostMapping("/orders/{operation}")
    public ResponseEntity<ServiceCallResponse> orders(@PathVariable String operation,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        return dispatch(OrderService.SERVICE_ID, operation, body);
    }

    @PostMapping("/payments/{operation}")
    public ResponseEntity<ServiceCallResponse> payments(@PathVariable String operation,
                                                        @RequestBody(required = false) Map<String, Object> body) {
        return dispatch(PaymentService.SERVICE_ID, operation, body);
    }

    private ResponseEntity<ServiceCallResponse> dispatch(String serviceId, String operation, Map<String, Object> body) {
        Map<String, Object> parameters = body != null ? body : Collections.emptyMap();
        ProxyExecution<ServiceResponse> execution = registry.proxyFor(serviceId).invoke(operation, parameters);
        return ResponseEntity.ok()
                .header(REQUEST_ID_HEADER, execution.requestId())
                .body(ServiceCallResponse.from(execution));
    }
}
