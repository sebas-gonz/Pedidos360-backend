package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditBffController {
    private final WebClient webClient;

    @Value("${clients.audit.url}")
    private String auditUrl;

    @GetMapping("/timeline")
    public Flux<Object> getTimeline(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String eventType) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host(auditUrl.replace("http://", ""))
                        .path("/api/audit/timeline")
                        .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                        .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                        .queryParamIfPresent("userEmail", java.util.Optional.ofNullable(userEmail))
                        .queryParamIfPresent("eventType", java.util.Optional.ofNullable(eventType))
                        .build())
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @GetMapping("/order/{uuid}")
    public Flux<Object> getOrderTimeline(@PathVariable String uuid) {
        return webClient.get()
                .uri(auditUrl + "/api/audit/order/" + uuid)
                .retrieve()
                .bodyToFlux(Object.class);
    }
}
