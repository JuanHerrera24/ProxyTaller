package com.observability.monitor.proxy;

import com.observability.monitor.exception.ResourceNotFoundException;
import com.observability.monitor.logging.CallLog;
import com.observability.monitor.logging.CallLogRepository;
import com.observability.monitor.logging.CallStatus;
import com.observability.monitor.service.inventory.InventoryService;
import com.observability.monitor.service.support.LatencySimulator;
import com.observability.monitor.service.support.ServiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoggingProxyTest {

    private CallLogRepository repository;
    private LoggingProxy<ServiceResponse> proxy;

    @BeforeEach
    void setUp() {
        repository = new CallLogRepository(100);
        proxy = new LoggingProxy<>(new InventoryService(LatencySimulator.none()), repository);
    }

    @Test
    void recordsSuccessfulCallWithResultAndTracingData() {
        ProxyExecution<ServiceResponse> execution =
                proxy.invoke(InventoryService.GET_PRODUCT, Map.of("sku", "SKU-1001"));

        assertThat(repository.findAll()).singleElement().satisfies(log -> {
            assertThat(log.requestId()).isEqualTo(execution.requestId());
            assertThat(UUID.fromString(log.requestId())).isNotNull();
            assertThat(log.serviceId()).isEqualTo(InventoryService.SERVICE_ID);
            assertThat(log.operation()).isEqualTo(InventoryService.GET_PRODUCT);
            assertThat(log.status()).isEqualTo(CallStatus.SUCCESS);
            assertThat(log.durationMs()).isGreaterThanOrEqualTo(0);
            assertThat(log.timestamp()).isNotNull();
            assertThat(log.parameters()).isEqualTo(Map.of("sku", "SKU-1001"));
            assertThat(log.response()).isEqualTo(execution.data());
            assertThat(log.error()).isNull();
        });
    }

    @Test
    void recordsFailureWithSummarizedStackTraceAndRethrows() {
        assertThatThrownBy(() -> proxy.invoke(InventoryService.GET_PRODUCT, Map.of("sku", "SKU-MISSING")))
                .isInstanceOf(ProxyInvocationException.class)
                .hasCauseInstanceOf(ResourceNotFoundException.class);

        CallLog log = repository.findAll().getFirst();
        assertThat(log.status()).isEqualTo(CallStatus.ERROR);
        assertThat(log.response()).isNull();
        assertThat(log.error().type()).isEqualTo("ResourceNotFoundException");
        assertThat(log.error().message()).contains("SKU-MISSING");
        assertThat(log.error().stackTrace()).isNotEmpty().hasSizeLessThanOrEqualTo(6);
    }

    @Test
    void executeReturnsTheWrappedServiceResult() {
        ServiceResponse response = proxy.execute(InventoryService.LIST_PRODUCTS, Map.of());

        assertThat(response.message()).isEqualTo("8 products found");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void assignsUniqueRequestIdToEveryCall() {
        String first = proxy.invoke(InventoryService.LIST_PRODUCTS).requestId();
        String second = proxy.invoke(InventoryService.LIST_PRODUCTS).requestId();

        assertThat(first).isNotEqualTo(second);
    }
}
