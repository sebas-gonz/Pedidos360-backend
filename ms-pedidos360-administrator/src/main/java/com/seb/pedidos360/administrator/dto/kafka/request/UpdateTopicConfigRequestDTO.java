package com.seb.pedidos360.administrator.dto.kafka.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record UpdateTopicConfigRequestDTO(
        @NotEmpty(message = "Debe especificar al menos una configuración a modificar")
        Map<String, String> configs
) {
}
