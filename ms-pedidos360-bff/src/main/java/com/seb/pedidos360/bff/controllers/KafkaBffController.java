package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kafka")
@RequiredArgsConstructor
public class KafkaBffController {
    private final WebClient webClient;

    @Value("${clients.administrator.url}")
    private String administratorUrl;

    private String baseUrl() {
        return administratorUrl + "/api/admin/kafka";
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> getClusterHealth() {
        return forward(webClient.get()
                .uri(baseUrl() + "/health"));
    }

    @GetMapping("/consumer-groups/details")
    public Mono<ResponseEntity<Object>> getConsumerGroupsWithLag() {
        return forward(webClient.get()
                .uri(baseUrl() + "/consumer-groups/details"));
    }

    @PostMapping("/consumer-groups/{groupId}/reset-offsets")
    public Mono<ResponseEntity<Object>> resetConsumerGroupOffsets(
            @PathVariable String groupId,
            @RequestBody Object request) {
        return forward(webClient.post()
                .uri(baseUrl() + "/consumer-groups/{groupId}/reset-offsets", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request));
    }

    @GetMapping("/topics/{topicName}/messages")
    public Mono<ResponseEntity<Object>> peekMessages(
            @PathVariable String topicName,
            @RequestParam(defaultValue = "20") int limit) {
        return forward(webClient.get()
                .uri(baseUrl() + "/topics/{topicName}/messages?limit={limit}", topicName, limit));
    }

    @DeleteMapping("/topics/{topicName}/messages")
    public Mono<ResponseEntity<Object>> purgeTopic(@PathVariable String topicName) {
        return forward(webClient.delete()
                .uri(baseUrl() + "/topics/{topicName}/messages", topicName));
    }

    @GetMapping("/cluster")
    public Mono<Object> getClusterInfo() {
        return webClient.get()
                .uri(administratorUrl + "/api/admin/kafka/cluster")
                .retrieve()
                .bodyToMono(Object.class);
    }

    @GetMapping("/topics")
    public Mono<List<String>> listTopics(@RequestParam(defaultValue = "false") boolean includeInternal) {
        return webClient.get()
                .uri(administratorUrl + "/api/admin/kafka/topics?includeInternal={includeInternal}", includeInternal)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});
    }

    @GetMapping("/topics/{topicName}")
    public Mono<Object> describeTopic(@PathVariable String topicName) {
        return webClient.get()
                .uri(administratorUrl + "/api/admin/kafka/topics/{topicName}", topicName)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Object> createTopic(@RequestBody Object request) {
        return webClient.post()
                .uri(administratorUrl + "/api/admin/kafka/topics")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @PutMapping("/topics/{topicName}/configs")
    public Mono<Object> updateTopicConfigs(
            @PathVariable String topicName,
            @RequestBody Object request) {
        return webClient.put()
                .uri(administratorUrl + "/api/admin/kafka/topics/{topicName}/configs", topicName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @PatchMapping("/topics/{topicName}/partitions")
    public Mono<Object> increasePartitions(
            @PathVariable String topicName,
            @RequestBody Object request) {
        return webClient.patch()
                .uri(administratorUrl + "/api/admin/kafka/topics/{topicName}/partitions", topicName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @DeleteMapping("/topics/{topicName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTopic(@PathVariable String topicName) {
        return webClient.delete()
                .uri(administratorUrl + "/api/admin/kafka/topics/{topicName}", topicName)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // --- Consumer Groups ---

    @GetMapping("/consumer-groups")
    public Flux<Object> listConsumerGroups() {
        return webClient.get()
                .uri(administratorUrl + "/api/admin/kafka/consumer-groups")
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @GetMapping("/consumer-groups/{groupId}")
    public Mono<Object> describeConsumerGroup(@PathVariable String groupId) {
        return webClient.get()
                .uri(administratorUrl + "/api/admin/kafka/consumer-groups/{groupId}", groupId)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteConsumerGroup(@PathVariable String groupId) {
        return webClient.delete()
                .uri(administratorUrl + "/api/admin/kafka/consumer-groups/{groupId}", groupId)
                .retrieve()
                .bodyToMono(Void.class);
    }

    private Mono<ResponseEntity<Object>> forward(WebClient.RequestHeadersSpec<?> request) {
        return request.exchangeToMono(response ->
                response.bodyToMono(Object.class)
                        .map(body -> ResponseEntity.status(response.statusCode()).body(body))
                        .defaultIfEmpty(ResponseEntity.status(response.statusCode()).<Object>build())
        );
    }
}
