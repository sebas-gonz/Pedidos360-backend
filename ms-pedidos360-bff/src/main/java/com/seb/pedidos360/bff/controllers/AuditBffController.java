package com.seb.pedidos360.bff.controllers;

import com.seb.pedidos360.bff.dto.queue.QueueRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditBffController {
    private final WebClient webClient;

    @Value("${clients.audit.url}")
    private String auditUrl;

    @GetMapping("/timeline")
    public Flux<Object> getTimeline(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        String targetUrl = UriComponentsBuilder.fromUriString(auditUrl + "/api/audits/timeline")
                .queryParamIfPresent("from", Optional.ofNullable(from))
                .queryParamIfPresent("to", Optional.ofNullable(to))
                .toUriString();

        return webClient.get()
                .uri(targetUrl)
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @GetMapping("/order/{uuid}")
    public Flux<Object> getOrderTimeline(@PathVariable String uuid) {
        return webClient.get()
                .uri(auditUrl + "/api/audits/order/" + uuid)
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @PostMapping("/queues")
    public Mono<ResponseEntity<Map<String, Object>>> createQueue(@Valid @RequestBody QueueRequestDTO request){
        return webClient.post()
                .uri(auditUrl + "/api/audits/queues")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){})
                .map(queueName -> ResponseEntity.ok().body(queueName))
                .doOnError(e -> System.out.println(e.getMessage()))
                .onErrorReturn(ResponseEntity.internalServerError().body(Map.of("Error", "Error al crear la cola")));
    }

    @DeleteMapping("/queues/{queuename}")
    public Mono<ResponseEntity<String>> deleteQueue(@PathVariable String queuename){
        return webClient.delete()
                .uri(auditUrl + "/api/audits/queues/" + queuename)
                .retrieve()
                .bodyToMono(String.class)
                .map(message -> ResponseEntity.ok("Cola eliminada"))
                .onErrorReturn(ResponseEntity.internalServerError().body("Error al eliminar la cola"));
    }

    @GetMapping("/queues")
    public Mono<ResponseEntity<Map<String, Object>>> getAllQueues(){
        return webClient.get()
                .uri(auditUrl + "/api/audits/queues")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>(){})
                .map(ResponseEntity::ok)
                .doOnError(e -> System.out.println(e.getMessage()))
                .onErrorReturn(ResponseEntity.internalServerError().body(Map.of("Error", "Error al obtener las colas")));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> streamAudits() {
        return webClient.get()
                .uri(auditUrl + "/api/audits/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<Object>>() {})
                .doOnError(e -> System.err.println("Error en bff audit stream proxy: " + e.getMessage()));
    }
}
