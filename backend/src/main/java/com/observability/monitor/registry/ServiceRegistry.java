package com.observability.monitor.registry;

import com.observability.monitor.exception.ResourceNotFoundException;
import com.observability.monitor.logging.CallLogRepository;
import com.observability.monitor.proxy.LoggingProxy;
import com.observability.monitor.proxy.ManagedService;
import com.observability.monitor.service.support.ServiceResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central catalog of microservices. Every registered service is exposed exclusively through its
 * {@link LoggingProxy}, which guarantees that no call bypasses auditing.
 */
@Component
public class ServiceRegistry {

    private final Map<String, LoggingProxy<ServiceResponse>> proxies;

    public ServiceRegistry(List<ManagedService<ServiceResponse>> services, CallLogRepository repository) {
        Map<String, LoggingProxy<ServiceResponse>> byId = new LinkedHashMap<>();
        services.stream()
                .sorted((left, right) -> left.serviceId().compareTo(right.serviceId()))
                .forEach(service -> {
                    if (byId.putIfAbsent(service.serviceId(), new LoggingProxy<>(service, repository)) != null) {
                        throw new IllegalStateException("Duplicate service id: " + service.serviceId());
                    }
                });
        this.proxies = Collections.unmodifiableMap(byId);
    }

    public LoggingProxy<ServiceResponse> proxyFor(String serviceId) {
        LoggingProxy<ServiceResponse> proxy = proxies.get(serviceId);
        if (proxy == null) {
            throw new ResourceNotFoundException("Service", serviceId);
        }
        return proxy;
    }

    public Collection<LoggingProxy<ServiceResponse>> proxies() {
        return proxies.values();
    }

    public List<String> serviceIds() {
        return List.copyOf(proxies.keySet());
    }
}
