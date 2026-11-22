package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/activemq")
@RequiredArgsConstructor
public class ActiveMQBffController {
    private final WebClient webClient;

    @Value("${clients.administrator.url}")
    private String administratorUrl;

    private String baseUrl(){
        return administratorUrl + "/api/admin/activemq";
    }
 
    
    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> getBrokerHealth() {
        return forward(webClient.get()
                .uri(baseUrl() + "/health"));
    }


    @GetMapping("/queues")
    public Mono<ResponseEntity<Object>> getAllQueues() {
        return forward(webClient.get()
                .uri(baseUrl() + "/queues"));
    }

    @PostMapping("/queues/{queueName}")
    public Mono<ResponseEntity<Object>> createQueue(@PathVariable String queueName) {
        return forward(webClient.post()
                .uri(baseUrl() + "/queues/{queueName}", queueName));
    }

    @DeleteMapping("/queues/{queueName}")
    public Mono<ResponseEntity<Object>> deleteQueue(@PathVariable String queueName) {
        return forward(webClient.delete()
                .uri(baseUrl() + "/queues/{queueName}", queueName));
    }

    @PostMapping("/queues/{queueName}/purge")
    public Mono<ResponseEntity<Object>> purgeQueue(@PathVariable String queueName) {
        return forward(webClient.post()
                .uri(baseUrl() + "/queues/{queueName}/purge", queueName));
    }

    @PostMapping("/queues/{queueName}/pause")
    public Mono<ResponseEntity<Object>> pauseQueue(@PathVariable String queueName) {
        return forward(webClient.post()
                .uri(baseUrl() + "/queues/{queueName}/pause", queueName));
    }

    @PostMapping("/queues/{queueName}/resume")
    public Mono<ResponseEntity<Object>> resumeQueue(@PathVariable String queueName) {
        return forward(webClient.post()
                .uri(baseUrl() + "/queues/{queueName}/resume", queueName));
    }

    @PostMapping("/queues/{queueName}/reset-stats")
    public Mono<ResponseEntity<Object>> resetQueueStats(@PathVariable String queueName) {
        return forward(webClient.post()
                .uri(baseUrl() + "/queues/{queueName}/reset-stats", queueName));
    }

    @GetMapping("/queues/{queueName}/messages")
    public Mono<ResponseEntity<Object>> browseQueue(@PathVariable String queueName) {
        return forward(webClient.get()
                .uri(baseUrl() + "/queues/{queueName}/messages", queueName));
    }

    @PostMapping("/queues/{queueName}/messages")
    public Mono<ResponseEntity<Object>> sendTestMessage(
            @PathVariable String queueName,
            @RequestBody Object request) {
        return forward(webClient.post()
                .uri(baseUrl() + "/queues/{queueName}/messages", queueName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request));
    }

    @DeleteMapping("/queues/{queueName}/messages")
    public Mono<ResponseEntity<Object>> removeMessage(
            @PathVariable String queueName,
            @RequestParam String messageId) {
        return forward(webClient.delete()
                .uri(baseUrl() + "/queues/{queueName}/messages?messageId={messageId}", queueName, messageId));
    }

    @PostMapping("/queues/{queueName}/messages/move")
    public Mono<ResponseEntity<Object>> moveMessage(
            @PathVariable String queueName,
            @RequestBody Object request) {
        return forward(webClient.post()
                .uri(baseUrl() + "/queues/{queueName}/messages/move", queueName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request));
    }

    @PostMapping("/dlq/{dlqName}/retry")
    public Mono<ResponseEntity<Object>> retryDlqMessages(@PathVariable String dlqName) {
        return forward(webClient.post()
                .uri(baseUrl() + "/dlq/{dlqName}/retry", dlqName));
    }

    @GetMapping("/topics")
    public Mono<ResponseEntity<Object>> getAllTopics() {
        return forward(webClient.get()
                .uri(baseUrl() + "/topics"));
    }

    @PostMapping("/topics/{topicName}")
    public Mono<ResponseEntity<Object>> createTopic(@PathVariable String topicName) {
        return forward(webClient.post()
                .uri(baseUrl() + "/topics/{topicName}", topicName));
    }

    @DeleteMapping("/topics/{topicName}")
    public Mono<ResponseEntity<Object>> deleteTopic(@PathVariable String topicName) {
        return forward(webClient.delete()
                .uri(baseUrl() + "/topics/{topicName}", topicName));
    }

    private Mono<ResponseEntity<Object>> forward(WebClient.RequestHeadersSpec<?> request) {
        return request.exchangeToMono(response ->
                response.bodyToMono(Object.class)
                        .map(body -> ResponseEntity.status(response.statusCode()).body(body))
                        .defaultIfEmpty(ResponseEntity.status(response.statusCode()).<Object>build())
        );
    }
}
