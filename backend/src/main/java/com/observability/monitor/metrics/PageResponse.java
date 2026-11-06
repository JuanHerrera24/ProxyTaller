package com.observability.monitor.metrics;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> slice(List<T> source, int page, int size) {
        int totalElements = source.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        return new PageResponse<>(
                List.copyOf(source.subList(fromIndex, toIndex)),
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1);
    }
}
