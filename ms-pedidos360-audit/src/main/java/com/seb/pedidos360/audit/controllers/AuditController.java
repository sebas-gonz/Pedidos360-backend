package com.seb.pedidos360.audit.controllers;

import com.seb.pedidos360.audit.dto.AuditIngestDTO;
import com.seb.pedidos360.audit.dto.AuditResponseDTO;
import com.seb.pedidos360.audit.dto.QueueRequestDTO;
import com.seb.pedidos360.audit.services.ActiveMQServiceImpl;
import com.seb.pedidos360.audit.services.AuditServcieImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {
    private final AuditServcieImpl auditService;
    private final WebClient jolokiaWebClient;
    private final ObjectMapper objectMapper;
    private final ActiveMQServiceImpl activeMQService;

    @GetMapping("/timeline")
    public Flux<AuditResponseDTO> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return auditService.getTimeline(from, to);
    }

    @PostMapping("/queues")
    public Mono<ResponseEntity<Map<String, Object>>> createQueue(@Valid @RequestBody QueueRequestDTO request) {
        return activeMQService.createQueue(request.queueName());
    }

    @DeleteMapping("/queues/{queueName}")
    public Mono<ResponseEntity<String>> deleteQueue(@PathVariable String queueName) {
        return activeMQService.deleteQueue(queueName);
    }

    @GetMapping("/order/{uuid}")
    public Flux<AuditResponseDTO> getOrderTimeline(@PathVariable UUID uuid) {
        return auditService.getOrderTimeline(uuid);
    }

    @GetMapping("/queues")
    public Mono<ResponseEntity<Map<String, Object>>> getAllQueues() {
        return activeMQService.getAllQueues();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AuditResponseDTO>> streamLiveAudits() {
        Flux<ServerSentEvent<AuditResponseDTO>> auditEvents = auditService.getAuditStream()
                .map(event -> ServerSentEvent.<AuditResponseDTO>builder()
                        .data(event)
                        .build());
        Flux<ServerSentEvent<AuditResponseDTO>> keepAlive = Flux.interval(Duration.ZERO, Duration.ofSeconds(15))
                .map(sequence -> ServerSentEvent.<AuditResponseDTO>builder()
                        .comment("keep-alive")
                        .build());

        return Flux.merge(auditEvents, keepAlive);
    }
}
