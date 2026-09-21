package com.seb.pedidos360.audit.controllers;

import com.seb.pedidos360.audit.dto.AuditIngestDTO;
import com.seb.pedidos360.audit.dto.AuditResponseDTO;
import com.seb.pedidos360.audit.services.AuditServcieImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditServcieImpl auditService;
    @GetMapping("/timeline")
    public Flux<AuditResponseDTO> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String eventType) {
        Instant fromDate = from != null ? from : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant toDate = to != null ? to : Instant.now();

        return auditService.getTimeline(fromDate, toDate, userEmail, eventType);
    }

    @GetMapping("/order/{uuid}")
    public Flux<AuditResponseDTO> getOrderTimeline(@PathVariable UUID uuid) {
        return auditService.getOrderTimeline(uuid);
    }
    @PostMapping("/ingest")
    public Mono<Void> ingestEvent(@RequestBody AuditIngestDTO ingestDTO) {
        return auditService.ingestEvent(ingestDTO);
    }
}
