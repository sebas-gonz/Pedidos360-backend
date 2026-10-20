package com.seb.pedidos360.audit.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table(name = "audit_events", schema = "audit")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuditEvent extends BaseEntity {
    @Id
    private Long id;
    private UUID orderUuid;
    private String eventType;
    private String userEmail;
    private String userRole;
    private String details;
}
