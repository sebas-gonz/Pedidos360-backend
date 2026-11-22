package com.seb.pedidos360.administrator.controller;

import com.seb.pedidos360.administrator.dto.activemq.request.MoveMessageRequestDTO;
import com.seb.pedidos360.administrator.dto.activemq.request.SendMessageRequestDTO;
import com.seb.pedidos360.administrator.dto.activemq.response.BrokerHealthDTO;
import com.seb.pedidos360.administrator.dto.activemq.response.QueueDetailDTO;
import com.seb.pedidos360.administrator.dto.activemq.response.QueueMessageDTO;
import com.seb.pedidos360.administrator.dto.activemq.response.TopicDetailDTO;
import com.seb.pedidos360.administrator.service.ActiveMQService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/activemq")
@RequiredArgsConstructor
public class ActiveMQController {

    private final ActiveMQService activeMQService;

    @GetMapping("/health")
    public Mono<ResponseEntity<BrokerHealthDTO>> getBrokerHealth() {
        return activeMQService.getBrokerHealth()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/queues")
    public Mono<ResponseEntity<List<QueueDetailDTO>>> getAllQueues() {
        return activeMQService.getAllQueues()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/queues/{queueName}")
    public Mono<ResponseEntity<Map<String, String>>> createQueue(@PathVariable String queueName) {
        return activeMQService.createQueue(queueName)
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Cola creada: " + queueName)));
    }

    @DeleteMapping("/queues/{queueName}")
    public Mono<ResponseEntity<Map<String, String>>> deleteQueue(@PathVariable String queueName) {
        return activeMQService.deleteQueue(queueName)
                .thenReturn(ResponseEntity.ok(Map.of("message", "Cola eliminada: " + queueName)));
    }

    @PostMapping("/queues/{queueName}/purge")
    public Mono<ResponseEntity<Map<String, String>>> purgeQueue(@PathVariable String queueName) {
        return activeMQService.purgeQueue(queueName)
                .thenReturn(ResponseEntity.ok(Map.of("message", "Cola purgada: " + queueName)));
    }

    @PostMapping("/queues/{queueName}/pause")
    public Mono<ResponseEntity<Map<String, String>>> pauseQueue(@PathVariable String queueName) {
        return activeMQService.pauseQueue(queueName)
                .thenReturn(ResponseEntity.ok(Map.of("message", "Cola pausada: " + queueName)));
    }

    @PostMapping("/queues/{queueName}/resume")
    public Mono<ResponseEntity<Map<String, String>>> resumeQueue(@PathVariable String queueName) {
        return activeMQService.resumeQueue(queueName)
                .thenReturn(ResponseEntity.ok(Map.of("message", "Cola reanudada: " + queueName)));
    }

    @PostMapping("/queues/{queueName}/reset-stats")
    public Mono<ResponseEntity<Map<String, String>>> resetQueueStats(@PathVariable String queueName) {
        return activeMQService.resetQueueStats(queueName)
                .thenReturn(ResponseEntity.ok(Map.of("message", "Estadísticas reiniciadas: " + queueName)));
    }

    @GetMapping("/queues/{queueName}/messages")
    public Mono<ResponseEntity<List<QueueMessageDTO>>> browseQueue(@PathVariable String queueName) {
        return activeMQService.browseQueue(queueName)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/queues/{queueName}/messages")
    public Mono<ResponseEntity<Map<String, String>>> sendTestMessage(
            @PathVariable String queueName,
            @RequestBody SendMessageRequestDTO request) {
        return activeMQService.sendTestMessage(queueName, request.payload())
                .map(messageId -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("messageId", messageId, "message", "Mensaje enviado a " + queueName)));
    }

    @DeleteMapping("/queues/{queueName}/messages")
    public Mono<ResponseEntity<Map<String, Object>>> removeMessage(
            @PathVariable String queueName,
            @RequestParam String messageId) {
        return activeMQService.removeMessage(queueName, messageId)
                .map(removed -> ResponseEntity.ok(Map.of(
                        "removed", removed,
                        "messageId", messageId
                )));
    }

    @PostMapping("/queues/{queueName}/messages/move")
    public Mono<ResponseEntity<Map<String, Object>>> moveMessage(
            @PathVariable String queueName,
            @RequestBody MoveMessageRequestDTO request) {
        return activeMQService.moveMessage(queueName, request.messageId(), request.targetQueue())
                .map(moved -> ResponseEntity.ok(Map.of(
                        "moved", moved,
                        "targetQueue", request.targetQueue()
                )));
    }

    @PostMapping("/dlq/{dlqName}/retry")
    public Mono<ResponseEntity<Map<String, Object>>> retryDlqMessages(@PathVariable String dlqName) {
        return activeMQService.retryDlqMessages(dlqName)
                .map(count -> ResponseEntity.ok(Map.of(
                        "retriedCount", count,
                        "message", "Mensajes reencolados desde " + dlqName + ": " + count
                )));
    }

    // 6. GESTIÓN DE TÓPICOS
    @GetMapping("/topics")
    public Mono<ResponseEntity<List<TopicDetailDTO>>> getAllTopics() {
        return activeMQService.getAllTopics()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/topics/{topicName}")
    public Mono<ResponseEntity<Map<String, String>>> createTopic(@PathVariable String topicName) {
        return activeMQService.createTopic(topicName)
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Tópico creado: " + topicName)));
    }

    @DeleteMapping("/topics/{topicName}")
    public Mono<ResponseEntity<Map<String, String>>> deleteTopic(@PathVariable String topicName) {
        return activeMQService.deleteTopic(topicName)
                .thenReturn(ResponseEntity.ok(Map.of("message", "Tópico eliminado: " + topicName)));
    }
}
