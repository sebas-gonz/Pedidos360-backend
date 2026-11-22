package com.seb.pedidos360.administrator.dto.kafka.response;

public record PartitionLagDTO(
        String topic,
        int partition,
        long currentOffset,
        long logEndOffset,
        long lag
) {
}
