package com.seb.pedidos360.administrator.service;

import com.seb.pedidos360.administrator.dto.kafka.request.ResetOffsetRequestDTO;
import com.seb.pedidos360.administrator.dto.kafka.response.ClusterHealthDTO;
import com.seb.pedidos360.administrator.dto.kafka.response.PartitionLagDTO;
import com.seb.pedidos360.administrator.dto.kafka.response.TopicMessageDTO;
import com.seb.pedidos360.administrator.dto.kafka.request.IncreasePartitionsRequestDTO;
import com.seb.pedidos360.administrator.dto.kafka.request.UpdateTopicConfigRequestDTO;
import com.seb.pedidos360.administrator.dto.kafka.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaService {
    private final AdminClient adminClient;

    @Value("${spring.kafka.bootstrap-servers}")
    private String boostrapServer;
    private static final long TIMEOUT_SECONDS = 10;
    private static final Set<String> KEY_CONFIGS = Set.of(
            "retention.ms", "cleanup.policy", "min.insync.replicas", "segment.bytes"
    );

    public Mono<ClusterInfoResponseDTO> describeCluster() {
        return Mono.fromCallable(() -> {
            DescribeClusterResult cluster = adminClient.describeCluster();
            String clusterId = cluster.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Node controller = cluster.controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Collection<Node> nodes = cluster.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<BrokerInfoDTO> brokers = nodes.stream()
                    .map(n -> new BrokerInfoDTO(n.id(), n.host(), n.port()))
                    .sorted(Comparator.comparingInt(BrokerInfoDTO::id))
                    .toList();

            return new ClusterInfoResponseDTO(clusterId, controller.id(), brokers);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<String>> listTopics(boolean includeInternal) {
        return Mono.fromCallable(() -> {
            ListTopicsOptions options = new ListTopicsOptions().listInternal(includeInternal);
            Set<String> names = adminClient.listTopics(options).names().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return names.stream().sorted().toList();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<TopicDetailResponseDTO> describeTopic(String topicName) {
        return Mono.fromCallable(() -> {
            try {
                TopicDescription desc = adminClient.describeTopics(List.of(topicName))
                        .allTopicNames()
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .get(topicName);

                ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                Config config = adminClient.describeConfigs(List.of(resource))
                        .all()
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .get(resource);

                Map<String, String> relevantConfigs = config.entries().stream()
                        .filter(e -> !e.isDefault() || KEY_CONFIGS.contains(e.name()))
                        .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value, (a, b) -> b, TreeMap::new));

                List<PartitionInfoDTO> partitions = desc.partitions().stream()
                        .map(p -> new PartitionInfoDTO(
                                p.partition(),
                                p.leader() != null ? p.leader().id() : -1,
                                p.replicas().stream().map(Node::id).toList(),
                                p.isr().stream().map(Node::id).toList()
                        ))
                        .toList();

                int replicationFactor = desc.partitions().isEmpty() ? 0 : desc.partitions().get(0).replicas().size();

                return new TopicDetailResponseDTO(
                        desc.name(),
                        desc.isInternal(),
                        desc.partitions().size(),
                        replicationFactor,
                        partitions,
                        relevantConfigs
                );
            } catch (ExecutionException e) {
                throw unwrapExecutionException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<TopicDetailResponseDTO> createTopic(CreateTopicRequestDTO request) {
        return Mono.fromCallable(() -> {
                    try {
                        NewTopic newTopic = new NewTopic(request.name(), request.partitions(), request.replicationFactor());
                        if (request.configs() != null && !request.configs().isEmpty()) {
                            newTopic.configs(request.configs());
                        }

                        adminClient.createTopics(List.of(newTopic)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        log.info("Tópico '{}' creado ({} particiones, {} réplicas)",
                                request.name(), request.partitions(), request.replicationFactor());
                        return request.name();
                    } catch (ExecutionException e) {
                        throw unwrapExecutionException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(topicName -> Mono.defer(() -> describeTopic(topicName))
                        .retryWhen(Retry.fixedDelay(6, Duration.ofMillis(250))
                                .filter(this::isTopicNotReady)));
    }

    private boolean isTopicNotReady(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode().value() == 404;
        }
        return ex instanceof UnknownTopicOrPartitionException
                || ex.getCause() instanceof UnknownTopicOrPartitionException;
    }

    public Mono<TopicDetailResponseDTO> updateTopicConfigs(String topicName, UpdateTopicConfigRequestDTO request) {
        return Mono.fromCallable(() -> {
                    try {
                        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                        Collection<AlterConfigOp> ops = request.configs().entrySet().stream()
                                .map(entry -> new AlterConfigOp(
                                        new ConfigEntry(entry.getKey(), entry.getValue()),
                                        AlterConfigOp.OpType.SET))
                                .toList();

                        adminClient.incrementalAlterConfigs(Map.of(resource, ops))
                                .all()
                                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        return topicName;
                    } catch (ExecutionException e) {
                        throw unwrapExecutionException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::describeTopic);
    }

    public Mono<TopicDetailResponseDTO> increasePartitions(String topicName, IncreasePartitionsRequestDTO request) {
        return Mono.fromCallable(() -> {
                    try {
                        adminClient.createPartitions(Map.of(topicName, NewPartitions.increaseTo(request.totalPartitions())))
                                .all()
                                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                        return topicName;
                    } catch (ExecutionException e) {
                        throw unwrapExecutionException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(this::describeTopic);
    }

    public Mono<Void> deleteTopic(String topicName) {
        return Mono.fromCallable(() -> {
            try {
                adminClient.deleteTopics(List.of(topicName)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.info("Tópico '{}' eliminado", topicName);
                return null;
            } catch (ExecutionException e) {
                throw unwrapExecutionException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<List<ConsumerGroupSummaryDTO>> listConsumerGroups() {
        return Mono.fromCallable(() -> {
                    List<GroupListing> listings = adminClient.listGroups(ListGroupsOptions.forConsumerGroups()).all().get().stream().toList();
                    return listings.stream()
                            .map(groupListing -> new ConsumerGroupSummaryDTO(
                                    groupListing.groupId(),
                                    groupListing.groupState().toString(),
                                    groupListing.isSimpleConsumerGroup()
                            ))
                            .sorted(Comparator.comparing(ConsumerGroupSummaryDTO::groupId))
                            .toList();
                }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ExecutionException.class, Throwable::getCause);
    }

    public Mono<ConsumerGroupDetailDTO> describeConsumerGroup(String groupId) {
        return Mono.fromCallable(() -> {
                    Map<String, ConsumerGroupDescription> described =
                            adminClient.describeConsumerGroups(List.of(groupId)).all().get();

                    ConsumerGroupDescription desc = described.get(groupId);
                    if (desc == null) {
                        throw new GroupIdNotFoundException("Consumer group no encontrado: " + groupId);
                    }

                    List<ConsumerMemberDTO> members = desc.members().stream()
                            .map(m -> new ConsumerMemberDTO(
                                    m.consumerId(),
                                    m.clientId(),
                                    m.host(),
                                    m.assignment().topicPartitions().stream()
                                            .sorted(Comparator.comparing(TopicPartition::topic)
                                                    .thenComparingInt(TopicPartition::partition))
                                            .map(tp -> tp.topic() + "-" + tp.partition())
                                            .toList()
                            ))
                            .toList();

                    Map<TopicPartition, OffsetAndMetadata> committedOffsets =
                            adminClient.listConsumerGroupOffsets(groupId)
                                    .partitionsToOffsetAndMetadata()
                                    .get();

                    List<ConsumerGroupOffsetDTO> offsets = List.of();
                    long totalLag = 0L;

                    if (!committedOffsets.isEmpty()) {
                        Map<TopicPartition, OffsetSpec> latestSpecs = committedOffsets.keySet().stream()
                                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

                        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                                adminClient.listOffsets(latestSpecs).all().get();

                        offsets = committedOffsets.entrySet().stream()
                                .map(entry -> {
                                    TopicPartition tp = entry.getKey();
                                    long currentOffset = entry.getValue().offset();
                                    ListOffsetsResult.ListOffsetsResultInfo endInfo = endOffsets.get(tp);
                                    long endOffset = (endInfo != null) ? endInfo.offset() : currentOffset;
                                    long lag = Math.max(0L, endOffset - currentOffset);

                                    return new ConsumerGroupOffsetDTO(
                                            tp.topic(),
                                            tp.partition(),
                                            currentOffset,
                                            endOffset,
                                            lag
                                    );
                                })
                                .sorted(Comparator.comparing(ConsumerGroupOffsetDTO::topic)
                                        .thenComparingInt(ConsumerGroupOffsetDTO::partition))
                                .toList();

                        totalLag = offsets.stream().mapToLong(ConsumerGroupOffsetDTO::lag).sum();
                    }

                    int coordinatorId = desc.coordinator() != null ? desc.coordinator().id() : -1;

                    return new ConsumerGroupDetailDTO(
                            desc.groupId(),
                            desc.groupState().name(),
                            coordinatorId,
                            desc.partitionAssignor(),
                            totalLag,
                            members,
                            offsets
                    );
                }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ExecutionException.class, Throwable::getCause);
    }

    public Mono<Void> deleteConsumerGroup(String groupId) {
        return Mono.fromCallable(() -> {
                    adminClient.deleteConsumerGroups(List.of(groupId)).all().get();
                    return null;
                }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ExecutionException.class, Throwable::getCause)
                .then();
    }

    private RuntimeException unwrapExecutionException(ExecutionException e) {
        if (e.getCause() instanceof RuntimeException runtime) {
            return runtime;
        }
        return new RuntimeException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e.getCause());
    }

    public Mono<ClusterHealthDTO> getClusterHealth() {
        return Mono.fromCallable(() -> {
            DescribeClusterResult cluster = adminClient.describeCluster();
            String clusterId = cluster.clusterId().get();
            int controllerId = cluster.controller().get().id();
            int activeBrokers = cluster.nodes().get().size();

            Set<String> topicNames = adminClient.listTopics(new ListTopicsOptions().listInternal(false))
                    .names().get();

            if (topicNames.isEmpty()) {
                return new ClusterHealthDTO(clusterId, controllerId, activeBrokers,
                        0, 0, 0, 0, Map.of(), Map.of(), Map.of());
            }

            Map<String, TopicDescription> descriptions = adminClient.describeTopics(topicNames)
                    .allTopicNames().get();

            int totalPartitions = 0;
            int urp = 0;
            int offline = 0;
            Map<Integer, Integer> leaderCount = new HashMap<>();
            Map<TopicPartition, OffsetSpec> earliestReq = new HashMap<>();
            Map<TopicPartition, OffsetSpec> latestReq = new HashMap<>();

            for (TopicDescription desc : descriptions.values()) {
                for (TopicPartitionInfo p : desc.partitions()) {
                    totalPartitions++;
                    if (p.leader() == null) {
                        offline++;
                    } else {
                        leaderCount.merge(p.leader().id(), 1, Integer::sum);
                    }
                    if (p.isr().size() < p.replicas().size()) {
                        urp++;
                    }
                    TopicPartition tp = new TopicPartition(desc.name(), p.partition());
                    earliestReq.put(tp, OffsetSpec.earliest());
                    latestReq.put(tp, OffsetSpec.latest());
                }
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestOffsets =
                    adminClient.listOffsets(earliestReq).all().get();
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets =
                    adminClient.listOffsets(latestReq).all().get();

            Map<String, Long> messageCounts = new TreeMap<>();
            for (TopicPartition tp : latestOffsets.keySet()) {
                long start = earliestOffsets.get(tp).offset();
                long end = latestOffsets.get(tp).offset();
                long count = Math.max(0L, end - start);
                messageCounts.merge(tp.topic(), count, Long::sum);
            }

            Map<String, Long> dltAlerts = messageCounts.entrySet().stream()
                    .filter(e -> e.getKey().toLowerCase().endsWith("dlt") && e.getValue() > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            return new ClusterHealthDTO(
                    clusterId, controllerId, activeBrokers,
                    topicNames.size(), totalPartitions, urp, offline,
                    leaderCount, messageCounts, dltAlerts
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<ConsumerGroupDetailDTO>> getConsumerGroupsWithLag() {
        return Mono.fromCallable(() -> {
            List<String> groupIds = adminClient.listGroups(ListGroupsOptions.forConsumerGroups())
                    .all()
                    .get()
                    .stream()
                    .map(GroupListing::groupId)
                    .sorted()
                    .toList();

            if (groupIds.isEmpty()) {
                return List.<ConsumerGroupDetailDTO>of();
            }

            Map<String, ConsumerGroupDescription> groupDescriptions =
                    adminClient.describeConsumerGroups(groupIds).all().get();

            Map<String, ListConsumerGroupOffsetsSpec> offsetSpecs = groupIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> new ListConsumerGroupOffsetsSpec()));

            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(offsetSpecs);

            Map<String, Map<TopicPartition, OffsetAndMetadata>> groupCommittedOffsets = new HashMap<>();
            Set<TopicPartition> allPartitions = new HashSet<>();

            for (String groupId : groupIds) {
                Map<TopicPartition, OffsetAndMetadata> committed =
                        offsetsResult.partitionsToOffsetAndMetadata(groupId).get();
                groupCommittedOffsets.put(groupId, committed);
                allPartitions.addAll(committed.keySet());
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = allPartitions.isEmpty()
                    ? Map.of()
                    : adminClient.listOffsets(
                    allPartitions.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()))
            ).all().get();

            List<ConsumerGroupDetailDTO> result = new ArrayList<>();
            for (String groupId : groupIds) {
                ConsumerGroupDescription desc = groupDescriptions.get(groupId);

                int coordinatorId = desc.coordinator() != null ? desc.coordinator().id() : -1;
                String partitionAssignor = (desc.partitionAssignor() != null && !desc.partitionAssignor().isBlank())
                        ? desc.partitionAssignor()
                        : "N/A";

                List<ConsumerMemberDTO> members = desc.members().stream()
                        .map(m -> new ConsumerMemberDTO(
                                m.consumerId(),
                                m.clientId(),
                                m.host(),
                                m.assignment().topicPartitions().stream()
                                        .map(tp -> tp.topic() + "#" + tp.partition())
                                        .sorted()
                                        .toList()
                        )).toList();

                Map<TopicPartition, OffsetAndMetadata> committed = groupCommittedOffsets.getOrDefault(groupId, Map.of());
                List<ConsumerGroupOffsetDTO> offsets = new ArrayList<>();
                long totalLag = 0;

                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committed.entrySet()) {
                    TopicPartition tp = entry.getKey();
                    long current = entry.getValue().offset();
                    long end = latestOffsets.containsKey(tp) ? latestOffsets.get(tp).offset() : current;
                    long lag = Math.max(0L, end - current);
                    totalLag += lag;
                    offsets.add(new ConsumerGroupOffsetDTO(tp.topic(), tp.partition(), current, end, lag));
                }
                offsets.sort(Comparator.comparing(ConsumerGroupOffsetDTO::topic)
                        .thenComparingInt(ConsumerGroupOffsetDTO::partition));

                result.add(new ConsumerGroupDetailDTO(
                        groupId,
                        desc.groupState().toString(),
                        coordinatorId,
                        partitionAssignor,
                        totalLag,
                        members,
                        offsets
                ));
            }
            return result;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> resetConsumerGroupOffsets(String groupId, ResetOffsetRequestDTO request) {
        return Mono.fromCallable(() -> {
            TopicDescription desc = adminClient.describeTopics(List.of(request.topic()))
                    .allTopicNames().get().get(request.topic());

            OffsetSpec spec = switch (request.strategy().toUpperCase()) {
                case "EARLIEST" -> OffsetSpec.earliest();
                case "LATEST" -> OffsetSpec.latest();
                case "TIMESTAMP" -> OffsetSpec.forTimestamp(Objects.requireNonNull(request.timestampMs(),
                        "timestampMs es obligatorio para estrategia TIMESTAMP"));
                default -> throw new IllegalArgumentException("Estrategia inválida: " + request.strategy());
            };

            Map<TopicPartition, OffsetSpec> offsetReq = desc.partitions().stream()
                    .map(p -> new TopicPartition(request.topic(), p.partition()))
                    .collect(Collectors.toMap(tp -> tp, tp -> spec));

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> targetOffsets =
                    adminClient.listOffsets(offsetReq).all().get();

            Map<TopicPartition, OffsetAndMetadata> newOffsets = new HashMap<>();
            targetOffsets.forEach((tp, info) -> {
                if (info.offset() >= 0) {
                    newOffsets.put(tp, new OffsetAndMetadata(info.offset()));
                }
            });

            adminClient.alterConsumerGroupOffsets(groupId, newOffsets).all().get();
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }


    // 5. PURGAR MENSAJES DE UN TÓPICO (Equivalente a vaciar cola en ActiveMQ)
    public Mono<Void> purgeTopic(String topicName) {
        return Mono.fromCallable(() -> {
            TopicDescription desc = adminClient.describeTopics(List.of(topicName))
                    .allTopicNames().get().get(topicName);

            Map<TopicPartition, OffsetSpec> latestReq = desc.partitions().stream()
                    .map(p -> new TopicPartition(topicName, p.partition()))
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets =
                    adminClient.listOffsets(latestReq).all().get();

            Map<TopicPartition, RecordsToDelete> toDelete = latestOffsets.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> RecordsToDelete.beforeOffset(e.getValue().offset())
                    ));

            adminClient.deleteRecords(toDelete).all().get();
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<List<TopicMessageDTO>> peekMessages(String topicName, int limit) {
        return Mono.fromCallable(() -> {
            int safeLimit = Math.min(Math.max(limit, 1), 100);
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServer);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, safeLimit);

            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                List<PartitionInfo> partitionInfos = consumer.partitionsFor(topicName);
                if (partitionInfos == null || partitionInfos.isEmpty()) {
                    return List.<TopicMessageDTO>of();
                }

                List<TopicPartition> partitions = partitionInfos.stream()
                        .map(p -> new TopicPartition(topicName, p.partition()))
                        .toList();

                consumer.assign(partitions);
                Map<TopicPartition, Long> beginning = consumer.beginningOffsets(partitions);
                Map<TopicPartition, Long> end = consumer.endOffsets(partitions);

                long totalExpected = 0;
                for (TopicPartition tp : partitions) {
                    long startOffset = Math.max(beginning.get(tp), end.get(tp) - safeLimit);
                    consumer.seek(tp, startOffset);
                    totalExpected += Math.max(0, end.get(tp) - startOffset);
                }

                if (totalExpected == 0) {
                    return List.<TopicMessageDTO>of();
                }

                List<TopicMessageDTO> messages = new ArrayList<>();
                int attempts = 0;
                while (messages.size() < totalExpected && attempts < 3) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(800));
                    for (ConsumerRecord<String, String> r : records) {
                        messages.add(new TopicMessageDTO(
                                r.partition(), r.offset(), r.timestamp(), r.key(), r.value()
                        ));
                    }
                    attempts++;
                }

                return messages.stream()
                        .sorted(Comparator.comparingLong(TopicMessageDTO::timestamp).reversed())
                        .limit(safeLimit)
                        .toList();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> updateTopicConfigs(String topicName, Map<String, String> configs) {
        return Mono.fromCallable(() -> {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            List<AlterConfigOp> ops = configs.entrySet().stream()
                    .map(e -> new AlterConfigOp(
                            new ConfigEntry(e.getKey(), e.getValue()),
                            AlterConfigOp.OpType.SET
                    ))
                    .toList();

            adminClient.incrementalAlterConfigs(Map.of(resource, ops)).all().get();
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
