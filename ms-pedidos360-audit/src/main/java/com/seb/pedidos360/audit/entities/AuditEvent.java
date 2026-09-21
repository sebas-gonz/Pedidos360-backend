package com.seb.pedidos360.audit.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table(name = "audit_events")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
    @Id
    private Long id;
    private UUID orderUuid;
    private String eventType;
    private String userEmail;
    private String userRole;
    private Instant eventDate;
    private String details;
}
