package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderBffController {
    private final WebClient webClient;

    @Value("${clients.orders.url}")
    private String ordersUrl;

    @GetMapping
    public Flux<Object> getAllOrders() {
        return webClient.get()
                .uri(ordersUrl + "/api/orders")
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @GetMapping("/users/{userId}")
    public Flux<Object> getOrdersByUser(@PathVariable String userId) {
        return webClient.get()
                .uri(ordersUrl + "/api/orders/users/" + userId)
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @PostMapping
    public Mono<Object> createOrder(@RequestBody Mono<Object> orderRequest,
                                    @AuthenticationPrincipal Jwt jwt) {
        return webClient.post()
                .uri(ordersUrl + "/api/orders")
                .body(orderRequest, Object.class)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @PutMapping("/{uuid}/status")
    public Mono<Object> updateOrderStatus(@PathVariable String uuid, @RequestBody Mono<Object> statusRequest) {

        return webClient.put()
                .uri(ordersUrl + "/api/orders/" + uuid + "/status")
                .body(statusRequest, Object.class)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @GetMapping("/status/{status}")
    public Flux<Object> getOrdersByStatus(@PathVariable String status) {
        return webClient.get()
                .uri(ordersUrl + "/api/orders/status/" + status)
                .retrieve()
                .bodyToFlux(Object.class);
    }
}
