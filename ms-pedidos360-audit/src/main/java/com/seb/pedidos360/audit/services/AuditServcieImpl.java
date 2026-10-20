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
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditServcieImpl {
    private final AuditEventRepository auditRepository;
    private final Sinks.Many<AuditResponseDTO> auditStream = Sinks.many().multicast().onBackpressureBuffer();

    public Mono<Void> ingestEvent(AuditIngestDTO dto) {
        AuditEvent event = AuditEvent.builder()
                .orderUuid(dto.orderUuid())
                .eventType(dto.eventType())
                .userEmail(dto.userEmail())
                .userRole(dto.userRole())
                .details(dto.details())
                .build();

        return auditRepository.save(event)
                .doOnSuccess(savedEvent -> {
                    AuditResponseDTO audit = this.mapToDTO(savedEvent);
                    auditStream.tryEmitNext(audit);
                }).then();
    }

    public Flux<AuditResponseDTO> getTimeline(Instant from, Instant to) {
        Instant fromDate = from != null ? from : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant toDate = to != null ? to : Instant.now();

        return auditRepository.findByCreatedAtBetween(fromDate, toDate)
                .map(this::mapToDTO);
    }

    public Flux<AuditResponseDTO> getOrderTimeline(UUID orderUuid) {
        return auditRepository.findByOrderUuidOrderByCreatedAtDesc(orderUuid)
                .map(this::mapToDTO);
    }

    public Flux<AuditResponseDTO> getAuditStream(){
        return auditStream.asFlux();
    }

    private AuditResponseDTO mapToDTO(AuditEvent event) {
        return new AuditResponseDTO(
                event.getOrderUuid(),
                event.getEventType(),
                event.getUserEmail(),
                event.getUserRole(),
                event.getCreatedAt(),
                event.getDetails()
        );
    }
}
