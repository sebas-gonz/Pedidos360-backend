package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportBffController {
    private final WebClient webClient;

    @Value("${clients.report.url}")
    private String reportUrl;

    @GetMapping("/kpis")
    public Mono<Object> getKpis(@RequestParam(defaultValue = "last24h") String range) {
        return webClient.get()
                .uri(reportUrl + "/api/reports/kpis?range={range}", range)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @GetMapping("/top-products")
    public Flux<Object> getTopProducts(@RequestParam(defaultValue = "last7d") String range) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host(reportUrl.replace("http://", ""))
                        .path("/api/report/top-products")
                        .queryParam("range", range)
                        .build())
                .retrieve()
                .bodyToFlux(Object.class);
    }
}
