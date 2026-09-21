package com.seb.pedidos360.audit.dto;

import java.util.UUID;

public record AuditIngestDTO(
        UUID orderUuid,
        String eventType,
        String userEmail,
        String userRole,
        String details
) {
}

