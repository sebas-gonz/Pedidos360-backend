package com.seb.pedidos360.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditResponseDTO(
        UUID orderUuid,
        String eventType,
        String userEmail,
        String userRole,
        Instant eventDate,
        String details
) {
}
