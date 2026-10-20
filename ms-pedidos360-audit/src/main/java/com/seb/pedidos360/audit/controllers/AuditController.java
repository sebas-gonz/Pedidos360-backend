package com.seb.pedidos360.audit.controllers;

import com.seb.pedidos360.audit.dto.AuditIngestDTO;
import com.seb.pedidos360.audit.dto.AuditResponseDTO;
import com.seb.pedidos360.audit.services.AuditServcieImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

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

    @GetMapping("/timeline")
    public Flux<AuditResponseDTO> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestHeader(name = "X-Customer-Email") String userEmail) {


        return auditService.getTimeline(from, to, userEmail);
    }

    @PostMapping("/queues/{queuename}")
    public Mono<ResponseEntity<Map<String, Object>>> createQueue(@PathVariable("queuename") String queueName) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", "org.apache.activemq:type=Broker,brokerName=localhost",
                "operation", "addQueue",
                "arguments", new String[]{queueName}
        );

        return jolokiaWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(stringResponse -> {
                    try {
                        return ResponseEntity.ok().body(objectMapper.readValue(stringResponse, new TypeReference<Map<String, Object>>(){}));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @DeleteMapping("/queues/{queueName}")
    public Mono<ResponseEntity<String>> deleteQueue(@PathVariable String queueName) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", "org.apache.activemq:type=Broker,brokerName=localhost",
                "operation", "removeQueue",
                "arguments", new String[]{queueName}
        );

        return jolokiaWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> ResponseEntity.ok("Cola eliminada"))
                .onErrorReturn(ResponseEntity.internalServerError().body("Error al eliminar la cola"));
    }

    @GetMapping("/order/{uuid}")
    public Flux<AuditResponseDTO> getOrderTimeline(@PathVariable UUID uuid) {
        return auditService.getOrderTimeline(uuid);
    }

    @GetMapping("/queues")
    public Mono<ResponseEntity<Map<String, Object>>> getAllQueues() {
        Map<String, Object> body = Map.of(
                "type", "read",
                "mbean", "org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=*"
        );

        return jolokiaWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(stringResponse -> {
                    try {
                        return ResponseEntity.ok().body(objectMapper.readValue(stringResponse, new TypeReference<Map<String, Object>>(){}));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
