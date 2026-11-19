package com.seb.pedidos360.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditIngestDTO(
        @NotNull(message = "El UUID de la entidad no puede ser nulo")
        UUID orderUuid,

        @NotBlank(message = "El tipo de evento es obligatorio")
        String eventType,

        @NotBlank(message = "El correo del usuario es obligatorio")
        String userEmail,

        @NotBlank(message = "El rol del usuario es obligatorio")
        String userRole,

        @NotBlank(message = "El detalle de auditoría es obligatorio")
        @Size(max = 500, message = "El detalle no puede superar los 500 caracteres")
        String details
) {
}

