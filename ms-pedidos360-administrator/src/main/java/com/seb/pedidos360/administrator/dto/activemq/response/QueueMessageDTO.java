package com.seb.pedidos360.administrator.dto.activemq.response;

public record QueueMessageDTO(
        String messageId,
        String text,
        long timestamp,
        long expiration,
        boolean redelivered,
        int priority,
        String originalDestination
) {
}
