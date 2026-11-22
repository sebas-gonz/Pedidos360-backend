package com.seb.pedidos360.audit.listeners;

import com.seb.pedidos360.audit.dto.AuditIngestDTO;
import com.seb.pedidos360.audit.services.AuditServcieImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditOrderListener {
    private final AuditServcieImpl auditService;

    @KafkaListener(topics = "${kafka.topics.orders}", groupId = "audit-service-group")
    public void consumeOrderEvent(@Valid AuditIngestDTO ingestDTO) {
        auditService.ingestEvent(ingestDTO)
                .doOnSuccess(v -> log.debug("Métricas guardadas para el evento: {}", ingestDTO.uuid()))
                .doOnError(e -> log.error("Error al procesar datos del evento {}: {}", ingestDTO.uuid(), e.getMessage()))
                .block(Duration.ofSeconds(10));
    }
}
