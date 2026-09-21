package com.seb.pedidos360.order.rest;

import com.seb.pedidos360.order.dto.report.ReportIngestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ReportClient {
    private final WebClient webClient;

    public ReportClient(WebClient.Builder webClientBuilder,
                        @Value("${app.clients.report.url}")
                        String clientsReportUrl) {
        this.webClient = webClientBuilder.baseUrl(clientsReportUrl).build();
    }

    public Mono<Void> ingestReport(ReportIngestDTO  ingestDTO) {
        return webClient.post()
                .uri("/api/reports/")
                .bodyValue(ingestDTO)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
