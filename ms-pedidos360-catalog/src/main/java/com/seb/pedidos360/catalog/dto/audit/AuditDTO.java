package com.seb.pedidos360.catalog.dto.audit;

import java.util.UUID;

public record AuditDTO(UUID uuid, String eventType, String userEmail, String userRole, String details) {
}
