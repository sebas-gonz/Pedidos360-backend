package com.seb.pedidos360.order.rest;

import com.seb.pedidos360.order.dto.notify.NotifyDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NotifyClient {
    private final WebClient webClient;
    public NotifyClient(WebClient.Builder webClient,
                        @Value("${app.clients.notify.url}")
                        String notifyUrl) {
        this.webClient = webClient.baseUrl(notifyUrl).build();
    }

    public Mono<Void> sentOrderCreatedNotification(NotifyDTO notify) {
        return webClient.post()
                .uri("/api/notify/created")
                .bodyValue(notify)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> setUpdatedOrderNotification(NotifyDTO notify) {
        return webClient.post()
                .uri("/api/notify/updated")
                .bodyValue(notify)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
