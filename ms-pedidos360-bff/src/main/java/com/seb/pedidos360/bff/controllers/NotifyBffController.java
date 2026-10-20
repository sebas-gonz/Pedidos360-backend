package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotifyBffController {
    private final WebClient webClient;

    @Value("${clients.notify.url}")
    private String notifyUrl;

    @GetMapping(value = "/kitchen/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> streamKitchenTickets() {
        return webClient.get()
                .uri(notifyUrl + "/api/notify/kitchen/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<Object>>() {})
                .doOnError(e -> System.err.println("Error en BFF Kitchen Stream: " + e.getMessage()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> streamUserNotifications() {
        return webClient.get()
                .uri(notifyUrl + "/api/notify/notification/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<Object>>() {})
                .doOnError(e -> System.err.println("Error en bff user notificacion proxy: " + e.getMessage()));
    }
}
