package com.seb.pedidos360.administrator.dto.kafka.response;

public record TopicMessageDTO(int partition,
                              long offset,
                              long timestamp,
                              String key,
                              String value) {
}
