package com.seb.pedidos360.administrator.dto.activemq.response;

public record BrokerHealthDTO(
        String brokerId,
        String brokerName,
        String brokerVersion,
        String uptime,
        int memoryPercentUsage,
        int storePercentUsage,
        int tempPercentUsage,
        long currentConnectionsCount,
        long totalConsumerCount,
        long totalProducerCount,
        long totalMessageCount,
        long totalEnqueueCount,
        long totalDequeueCount
) {
}
