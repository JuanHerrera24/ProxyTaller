package com.observability.monitor.web.dto;

import java.util.List;

public record ServiceDescriptor(String serviceId, String displayName, String endpoint, List<String> operations) {
}
