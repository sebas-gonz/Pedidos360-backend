package com.seb.pedidos360.audit.services;

import com.seb.pedidos360.audit.dto.AuditIngestDTO;
import com.seb.pedidos360.audit.dto.AuditResponseDTO;
import com.seb.pedidos360.audit.entities.AuditEvent;
import com.seb.pedidos360.audit.repositories.AuditEventRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditServcieImpl {
    private final AuditEventRepository auditRepository;

    public Mono<Void> ingestEvent(AuditIngestDTO dto) {
        AuditEvent event = AuditEvent.builder()
                .orderUuid(dto.orderUuid())
                .eventType(dto.eventType())
                .userEmail(dto.userEmail())
                .userRole(dto.userRole())
                .details(dto.details())
                .eventDate(Instant.now())
                .build();

        return auditRepository.save(event).then();
    }
    public Flux<AuditResponseDTO> getTimeline(Instant from, Instant to, String userEmail, String eventType) {
        return auditRepository.findByEventDateBetween(from, to)
                .filter(event -> userEmail == null || userEmail.isBlank() || event.getUserEmail().equalsIgnoreCase(userEmail))
                .filter(event -> eventType == null || eventType.isBlank() || event.getEventType().equalsIgnoreCase(eventType))
                .map(this::mapToDTO);
    }
    public Flux<AuditResponseDTO> getOrderTimeline(UUID orderUuid) {
        return auditRepository.findByOrderUuidOrderByEventDateDesc(orderUuid)
                .map(this::mapToDTO);
    }

    private AuditResponseDTO mapToDTO(AuditEvent event) {
        return new AuditResponseDTO(
                event.getOrderUuid(),
                event.getEventType(),
                event.getUserEmail(),
                event.getUserRole(),
                event.getEventDate(),
                event.getDetails()
        );
    }
}
