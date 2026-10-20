package com.seb.pedidos360.audit.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class ActiveMQServiceImpl {
    private final WebClient jolokiaWebClient;
    private final ObjectMapper objectMapper;

    public Mono<ResponseEntity<Map<String, Object>>> createQueue(String queueName) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", "org.apache.activemq:type=Broker,brokerName=localhost",
                "operation", "addQueue",
                "arguments", new String[]{queueName}
        );

        return executeJolokiaRequest(body, "Cola creada");
    }

    public Mono<ResponseEntity<String>> deleteQueue(String queueName) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", "org.apache.activemq:type=Broker,brokerName=localhost",
                "operation", "removeQueue",
                "arguments", new String[]{queueName}
        );

        return jolokiaWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> ResponseEntity.ok("Cola eliminada"))
                .onErrorReturn(ResponseEntity.internalServerError().body("Error al eliminar la cola"));
    }

    public Mono<ResponseEntity<Map<String, Object>>> getAllQueues() {
        Map<String, Object> body = Map.of(
                "type", "read",
                "mbean", "org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=*"
        );

        return executeJolokiaRequest(body, null);
    }

    private Mono<ResponseEntity<Map<String, Object>>> executeJolokiaRequest(Map<String, Object> body, String successMessage) {
        return jolokiaWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseJson -> {
                    try {
                        Map<String, Object> parsedMap = objectMapper.readValue(
                                responseJson,
                                new TypeReference<Map<String, Object>>() {}
                        );
                        return ResponseEntity.ok(parsedMap);
                    } catch (Exception e) {
                        throw new RuntimeException("Error al parsear respuesta de Jolokia", e);
                    }
                })
                .doOnError(e -> System.err.println("Erro en ActiveMQ " + e.getMessage()))
                .onErrorReturn(ResponseEntity.internalServerError().body(Map.of("error", (Object) "Error en operación de mensajería")));
    }
}
