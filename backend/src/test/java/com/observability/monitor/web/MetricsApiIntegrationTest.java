package com.observability.monitor.web;

import com.observability.monitor.logging.CallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetricsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CallLogRepository repository;

    @BeforeEach
    void clearLogs() {
        repository.clear();
    }

    @Test
    void serviceCallIsProxiedAndExposedInLogsAndSummary() throws Exception {
        mockMvc.perform(post("/api/services/inventory/check-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.available", is(true)));

        mockMvc.perform(get("/api/metrics/logs").param("service", "inventory").param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].requestId", notNullValue()))
                .andExpect(jsonPath("$.content[0].parameters.sku", is("SKU-1001")));

        mockMvc.perform(get("/api/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services", hasSize(3)))
                .andExpect(jsonPath("$.totalCalls", is(1)));
    }

    @Test
    void failedCallReturnsTracedErrorAndIsLogged() throws Exception {
        mockMvc.perform(post("/api/services/orders/get-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"ORD-404\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.serviceId", is("orders")));

        mockMvc.perform(get("/api/metrics/logs").param("status", "ERROR"))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].error.type", is("ResourceNotFoundException")));
    }

    @Test
    void simulateLoadGeneratesFiftyCalls() throws Exception {
        mockMvc.perform(post("/api/metrics/simulate-load"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCalls", is(50)));

        mockMvc.perform(get("/api/metrics/summary"))
                .andExpect(jsonPath("$.totalCalls", is(50)));

        mockMvc.perform(get("/api/metrics/logs").param("size", "10").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalPages", is(5)));
    }

    @Test
    void rejectsInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/metrics/logs").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/metrics/logs").param("from", "not-a-date"))
                .andExpect(status().isBadRequest());
    }
}
