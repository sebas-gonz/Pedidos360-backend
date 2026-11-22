package com.seb.pedidos360.administrator.dto.kafka.request;

import jakarta.validation.constraints.Min;

public record IncreasePartitionsRequestDTO(
        @Min(value = 2, message = "El nuevo número de particiones debe ser mayor al actual")
        int totalPartitions
) {
}
