package com.seb.pedidos360.administrator.controller;

import com.seb.pedidos360.administrator.dto.kafka.request.IncreasePartitionsRequestDTO;
import com.seb.pedidos360.administrator.dto.kafka.request.ResetOffsetRequestDTO;
import com.seb.pedidos360.administrator.dto.kafka.request.UpdateTopicConfigRequestDTO;
import com.seb.pedidos360.administrator.dto.kafka.response.*;
import com.seb.pedidos360.administrator.service.KafkaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/kafka")
@RequiredArgsConstructor
public class KafkaController {
    private final KafkaService kafkaAdminService;

    @GetMapping("/cluster")
    public Mono<ClusterInfoResponseDTO> getClusterInfo() {
        return kafkaAdminService.describeCluster();
    }

    @GetMapping("/topics")
    public Mono<List<String>> listTopics(@RequestParam(defaultValue = "false") boolean includeInternal) {
        return kafkaAdminService.listTopics(includeInternal);
    }

    @GetMapping("/topics/{topicName}")
    public Mono<TopicDetailResponseDTO> describeTopic(@PathVariable String topicName) {
        return kafkaAdminService.describeTopic(topicName);
    }

    @PostMapping("/topics")
    public Mono<ResponseEntity<TopicDetailResponseDTO>> createTopic(@Valid @RequestBody CreateTopicRequestDTO request) {
        return kafkaAdminService.createTopic(request)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @PutMapping("/topics/{topicName}/configs")
    public Mono<TopicDetailResponseDTO> updateTopicConfigs(
            @PathVariable String topicName,
            @Valid @RequestBody UpdateTopicConfigRequestDTO request) {
        return kafkaAdminService.updateTopicConfigs(topicName, request);
    }

    @PatchMapping("/topics/{topicName}/partitions")
    public Mono<TopicDetailResponseDTO> increasePartitions(
            @PathVariable String topicName,
            @Valid @RequestBody IncreasePartitionsRequestDTO request) {
        return kafkaAdminService.increasePartitions(topicName, request);
    }

    @DeleteMapping("/topics/{topicName}")
    public Mono<ResponseEntity<Void>> deleteTopic(@PathVariable String topicName) {
        return kafkaAdminService.deleteTopic(topicName)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/consumer-groups")
    public Mono<ResponseEntity<List<ConsumerGroupSummaryDTO>>> listConsumerGroups() {
        return kafkaAdminService.listConsumerGroups()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/consumer-groups/{groupId}")
    public Mono<ResponseEntity<ConsumerGroupDetailDTO>> describeConsumerGroup(@PathVariable String groupId) {
        return kafkaAdminService.describeConsumerGroup(groupId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public Mono<ResponseEntity<Void>> deleteConsumerGroup(@PathVariable String groupId) {
        return kafkaAdminService.deleteConsumerGroup(groupId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<ClusterHealthDTO>> getClusterHealth() {
        return kafkaAdminService.getClusterHealth()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/consumer-groups/details")
    public Mono<ResponseEntity<List<ConsumerGroupDetailDTO>>> getConsumerGroupsWithLag() {
        return kafkaAdminService.getConsumerGroupsWithLag()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/consumer-groups/{groupId}/reset-offsets")
    public Mono<ResponseEntity<Map<String, String>>> resetConsumerGroupOffsets(
            @PathVariable String groupId,
            @Valid @RequestBody ResetOffsetRequestDTO request) {
        return kafkaAdminService.resetConsumerGroupOffsets(groupId, request)
                .thenReturn(ResponseEntity.ok(Map.of(
                        "message", "Offsets reseteados para el grupo " + groupId + " (" + request.strategy() + ")"
                )));
    }

    @GetMapping("/topics/{topicName}/messages")
    public Mono<ResponseEntity<List<TopicMessageDTO>>> peekMessages(
            @PathVariable String topicName,
            @RequestParam(defaultValue = "20") int limit) {
        return kafkaAdminService.peekMessages(topicName, limit)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/topics/{topicName}/messages")
    public Mono<ResponseEntity< Map<String, String> >> purgeTopic(@PathVariable String topicName) {
        return kafkaAdminService.purgeTopic(topicName)
                .thenReturn(ResponseEntity.ok(Map.of(
                        "message", "Mensajes purgados del tópico: " + topicName
                )));
    }
}
