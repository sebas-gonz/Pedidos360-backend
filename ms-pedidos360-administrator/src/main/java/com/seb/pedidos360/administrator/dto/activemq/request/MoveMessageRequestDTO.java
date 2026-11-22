package com.seb.pedidos360.administrator.dto.activemq.request;

public record MoveMessageRequestDTO(String messageId, String targetQueue) {
}
