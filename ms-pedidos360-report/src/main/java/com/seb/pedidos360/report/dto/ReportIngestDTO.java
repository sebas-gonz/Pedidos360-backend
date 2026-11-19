package com.seb.pedidos360.report.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportIngestDTO(
        @NotNull(message = "El orderUuid no puede ser nulo")
        UUID orderUuid,

        @NotBlank(message = "El estado no puede estar vacío")
        String status,

        @NotNull(message = "El monto total es obligatorio")
        @PositiveOrZero(message = "El monto total no puede ser negativo")
        BigDecimal totalAmount,

        @NotNull(message = "El timestamp es obligatorio")
        Instant timestamp,

        @NotNull(message = "La lista de items no puede ser nula")
        List<@Valid ProductQuantityDTO> items
){

}
