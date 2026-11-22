package com.seb.pedidos360.order.dto.audit;

import java.util.UUID;

public record AuditIngestDTO(UUID uuid, String eventType, String userEmail, String userRole, String details) {
}
