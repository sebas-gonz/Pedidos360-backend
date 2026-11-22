package com.seb.pedidos360.administrator.dto.activemq.response;

public record QueueDetailDTO(
        String name,
        long queueSize,
        long consumerCount,
        long enqueueCount,
        long dequeueCount,
        long inFlightCount,
        long expiredCount,
        double averageEnqueueTime,
        int memoryPercentUsage,
        boolean paused,
        boolean dlq
) {
}
