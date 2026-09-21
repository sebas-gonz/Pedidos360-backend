package com.seb.pedidos360.order.rest;

import com.seb.pedidos360.order.dto.audit.AuditIngestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuditClient {
    private final WebClient webClient;

    public AuditClient(WebClient.Builder webClientBuilder,
                       @Value("${app.clients.audit.url}")
                       String clientsAuditUrl) {
        this.webClient = webClientBuilder.baseUrl(clientsAuditUrl).build();
    }

    public Mono<Void> ingestAudit(AuditIngestDTO  auditIngestDTO) {
        return webClient.post()
                .uri("/api/audits/ingest")
                .bodyValue(auditIngestDTO)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
