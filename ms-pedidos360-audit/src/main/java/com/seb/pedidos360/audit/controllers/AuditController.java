package com.seb.pedidos360.audit.controllers;

import com.seb.pedidos360.audit.dto.AuditResponseDTO;
import com.seb.pedidos360.audit.services.AuditServcieImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {
    private final AuditServcieImpl auditService;

    @GetMapping("/timeline")
    public Flux<AuditResponseDTO> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return auditService.getTimeline(from, to);
    }



    @GetMapping("/order/{uuid}")
    public Flux<AuditResponseDTO> getOrderTimeline(@PathVariable UUID uuid) {
        return auditService.getOrderTimeline(uuid);
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
