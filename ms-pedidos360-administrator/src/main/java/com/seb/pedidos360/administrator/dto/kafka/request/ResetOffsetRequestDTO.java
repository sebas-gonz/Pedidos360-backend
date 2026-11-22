package com.seb.pedidos360.administrator.dto.kafka.request;

public record ResetOffsetRequestDTO(
        String topic,
        String strategy,
        Long timestampMs
) {
}
