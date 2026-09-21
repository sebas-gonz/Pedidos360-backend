package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductBffController {
    private final WebClient webClient;

    @Value("${clients.catalog.url}")
    private String catalogUrl;

    @GetMapping
    public Flux<Object> getAllProducts() {
        return webClient.get()
                .uri(catalogUrl + "/api/catalog/products")
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @PostMapping
    public Mono<Object> createProduct(@RequestBody Mono<Object> productRequest) {
        return webClient.post()
                .uri(catalogUrl + "/api/catalog/products")
                .body(productRequest, Object.class)
                .retrieve()
                .bodyToMono(Object.class);
    }
    @PutMapping("/products/{uuid}")
    public Mono<Object> updateProduct(@PathVariable UUID uuid, @RequestBody Mono<Object> productRequest) {
        return webClient.put()
                .uri(catalogUrl + "/api/catalog/products/" + uuid)
                .body(productRequest, Object.class)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @GetMapping("/products/{uuid}")
    public Mono<Object> getProduct(@PathVariable UUID uuid) {
        return webClient.get()
                .uri(catalogUrl + "/api/catalog/products/" + uuid)
                .retrieve()
                .bodyToMono(Object.class);
    }
}
