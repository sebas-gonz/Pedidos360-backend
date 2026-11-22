package com.seb.pedidos360.administrator.dto.kafka.response;

public record ConsumerGroupSummaryDTO(
        String groupId,
        String state,
        boolean simpleConsumerGroup
) {
}
