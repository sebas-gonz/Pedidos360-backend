package com.seb.pedidos360.administrator.dto.activemq.response;

public record TopicDetailDTO(
        String name,
        long consumerCount,
        long enqueueCount,
        long dequeueCount,
        long inFlightCount,
        int memoryPercentUsage
) {
}
