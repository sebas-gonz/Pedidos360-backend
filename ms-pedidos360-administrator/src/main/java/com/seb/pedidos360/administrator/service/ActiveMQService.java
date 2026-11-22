package com.seb.pedidos360.administrator.service;

import com.seb.pedidos360.administrator.dto.activemq.response.BrokerHealthDTO;
import com.seb.pedidos360.administrator.dto.activemq.response.QueueDetailDTO;
import com.seb.pedidos360.administrator.dto.activemq.response.QueueMessageDTO;
import com.seb.pedidos360.administrator.dto.activemq.response.TopicDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveMQService {
    private final WebClient jolokiaWebClient;
    private final ObjectMapper objectMapper;

    @Value("${activemq.jolokia.url}")
    private List<String> jolokiaUrls;

    @Value("${activemq.jolokia.broker-name:localhost}")
    private String brokerName;

    private final AtomicInteger activeNodeIndex = new AtomicInteger(0);

    private String brokerMBean() {
        return "org.apache.activemq:type=Broker,brokerName=" + brokerName;
    }

    private String queueMBean(String queueName) {
        return brokerMBean() + ",destinationType=Queue,destinationName=" + queueName;
    }

    public Mono<BrokerHealthDTO> getBrokerHealth() {
        Map<String, Object> body = Map.of(
                "type", "read",
                "mbean", brokerMBean()
        );

        return executeJolokia(body).map(res -> {
            Map<String, Object> v = asMap(res.get("value"));
            return new BrokerHealthDTO(
                    asString(v.get("BrokerId")),
                    asString(v.get("BrokerName")),
                    asString(v.get("BrokerVersion")),
                    asString(v.get("Uptime")),
                    asInt(v.get("MemoryPercentUsage")),
                    asInt(v.get("StorePercentUsage")),
                    asInt(v.get("TempPercentUsage")),
                    asLong(v.get("CurrentConnectionsCount")),
                    asLong(v.get("TotalConsumerCount")),
                    asLong(v.get("TotalProducerCount")),
                    asLong(v.get("TotalMessageCount")),
                    asLong(v.get("TotalEnqueueCount")),
                    asLong(v.get("TotalDequeueCount"))
            );
        });
    }

    public Mono<List<QueueDetailDTO>> getAllQueues() {
        Map<String, Object> body = Map.of(
                "type", "read",
                "mbean", queueMBean("*")
        );

        return executeJolokia(body).map(res -> {
            Map<String, Object> queuesMap = asMap(res.get("value"));
            List<QueueDetailDTO> list = new ArrayList<>();

            for (Object entryVal : queuesMap.values()) {
                Map<String, Object> q = asMap(entryVal);
                String name = asString(q.get("Name"));
                list.add(new QueueDetailDTO(
                        name,
                        asLong(q.get("QueueSize")),
                        asLong(q.get("ConsumerCount")),
                        asLong(q.get("EnqueueCount")),
                        asLong(q.get("DequeueCount")),
                        asLong(q.get("InFlightCount")),
                        asLong(q.get("ExpiredCount")),
                        asDouble(q.get("AverageEnqueueTime")),
                        asInt(q.get("MemoryPercentUsage")),
                        asBoolean(q.get("Paused")),
                        name.toUpperCase().contains("DLQ")
                ));
            }
            list.sort(Comparator.comparing(QueueDetailDTO::name));
            return list;
        });
    }

    public Mono<Void> createQueue(String queueName) {
        return execBrokerOperation("addQueue", List.of(queueName));
    }

    public Mono<Void> deleteQueue(String queueName) {
        return execBrokerOperation("removeQueue", List.of(queueName));
    }

    public Mono<List<TopicDetailDTO>> getAllTopics() {
        Map<String, Object> body = Map.of(
                "type", "read",
                "mbean", brokerMBean() + ",destinationType=Topic,destinationName=*"
        );

        return executeJolokia(body).map(res -> {
            Map<String, Object> topicsMap = asMap(res.get("value"));
            List<TopicDetailDTO> list = new ArrayList<>();

            for (Object entryVal : topicsMap.values()) {
                Map<String, Object> t = asMap(entryVal);
                String name = asString(t.get("Name"));
                if (name.startsWith("ActiveMQ.Advisory.")) continue;
                list.add(new TopicDetailDTO(
                        name,
                        asLong(t.get("ConsumerCount")),
                        asLong(t.get("EnqueueCount")),
                        asLong(t.get("DequeueCount")),
                        asLong(t.get("InFlightCount")),
                        asInt(t.get("MemoryPercentUsage"))
                ));
            }
            list.sort(Comparator.comparing(TopicDetailDTO::name));
            return list;
        });
    }

    public Mono<Void> createTopic(String topicName) {
        return execBrokerOperation("addTopic", List.of(topicName));
    }

    public Mono<Void> deleteTopic(String topicName) {
        return execBrokerOperation("removeTopic", List.of(topicName));
    }

    public Mono<Void> purgeQueue(String queueName) {
        return execQueueOperation(queueName, "purge", List.of());
    }

    public Mono<Void> pauseQueue(String queueName) {
        return execQueueOperation(queueName, "pause", List.of());
    }

    public Mono<Void> resumeQueue(String queueName) {
        return execQueueOperation(queueName, "resume", List.of());
    }

    public Mono<Void> resetQueueStats(String queueName) {
        return execQueueOperation(queueName, "resetStatistics", List.of());
    }

    public Mono<List<QueueMessageDTO>> browseQueue(String queueName) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", queueMBean(queueName),
                "operation", "browse()",
                "arguments", List.of()
        );

        return executeJolokia(body).map(res -> {
            Object raw = res.get("value");
            if (!(raw instanceof List<?> items)) return List.<QueueMessageDTO>of();

            List<QueueMessageDTO> messages = new ArrayList<>();
            for (Object item : items) {
                Map<String, Object> m = asMap(item);
                messages.add(new QueueMessageDTO(
                        asString(m.get("JMSMessageID")),
                        asString(m.get("Text")),
                        asLong(m.get("JMSTimestamp")),
                        asLong(m.get("JMSExpiration")),
                        asBoolean(m.get("JMSRedelivered")),
                        asInt(m.get("JMSPriority")),
                        asString(m.get("OriginalDestination"))
                ));
            }
            return messages;
        });
    }

    public Mono<Integer> retryDlqMessages(String dlqName) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", queueMBean(dlqName),
                "operation", "retryMessages",
                "arguments", List.of()
        );
        return executeJolokia(body).map(res -> asInt(res.get("value")));
    }

    public Mono<Boolean> moveMessage(String sourceQueue, String messageId, String targetQueue) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", queueMBean(sourceQueue),
                "operation", "moveMessageTo",
                "arguments", List.of(messageId, targetQueue)
        );
        return executeJolokia(body).map(res -> asBoolean(res.get("value")));
    }

    public Mono<Boolean> removeMessage(String queueName, String messageId) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", queueMBean(queueName),
                "operation", "removeMessage",
                "arguments", List.of(messageId)
        );
        return executeJolokia(body).map(res -> asBoolean(res.get("value")));
    }

    public Mono<String> sendTestMessage(String queueName, String payload) {
        Map<String, Object> body = Map.of(
                "type", "exec",
                "mbean", queueMBean(queueName),
                "operation", "sendTextMessage(java.lang.String)",
                "arguments", List.of(payload)
        );
        return executeJolokia(body).map(res -> asString(res.get("value")));
    }
    // --- HELPERS INTERNOS ---
    private Mono<Void> execBrokerOperation(String operation, List<Object> args) {
        return executeJolokia(Map.of(
                "type", "exec",
                "mbean", brokerMBean(),
                "operation", operation,
                "arguments", args
        )).then();
    }

    private Mono<Void> execQueueOperation(String queueName, String operation, List<Object> args) {
        return executeJolokia(Map.of(
                "type", "exec",
                "mbean", queueMBean(queueName),
                "operation", operation,
                "arguments", args
        )).then();
    }

    private Mono<Map<String, Object>> executeJolokia(Map<String, Object> body) {
        return Mono.defer(() -> {
            if (jolokiaUrls == null || jolokiaUrls.isEmpty()) {
                return Mono.error(new IllegalStateException("No hay URLs configuradas en activemq.jolokia.url"));
            }
            int startIndex = activeNodeIndex.get() % jolokiaUrls.size();
            return tryJolokiaNode(body, startIndex, 0);
        });
    }

    private Mono<Map<String, Object>> tryJolokiaNode(Map<String, Object> body, int currentIndex, int attempt) {
        String url = jolokiaUrls.get(currentIndex).trim();

        return jolokiaWebClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    activeNodeIndex.set(currentIndex);
                    Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
                    int status = asInt(parsed.get("status"));
                    if (status >= 400) {
                        throw new RuntimeException("Error Jolokia (" + status + "): " + parsed.get("error"));
                    }
                    return parsed;
                })
                .onErrorResume(WebClientRequestException.class, ex -> {
                    int nextAttempt = attempt + 1;
                    if (nextAttempt >= jolokiaUrls.size()) {
                        log.error("Ningún nodo de ActiveMQ Jolokia respondió. Último error en {}: {}", url, ex.getMessage());
                        return Mono.error(ex);
                    }
                    int nextIndex = (currentIndex + 1) % jolokiaUrls.size();
                    log.warn("Jolokia en {} no disponible ({}). Haciendo failover hacia {}...",
                            url, ex.getMessage(), jolokiaUrls.get(nextIndex).trim());
                    return tryJolokiaNode(body, nextIndex, nextAttempt);
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        return obj instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String asString(Object obj) { return obj != null ? obj.toString() : ""; }
    private long asLong(Object obj) { return obj instanceof Number n ? n.longValue() : 0L; }
    private int asInt(Object obj) { return obj instanceof Number n ? n.intValue() : 0; }
    private double asDouble(Object obj) { return obj instanceof Number n ? n.doubleValue() : 0.0; }
    private boolean asBoolean(Object obj) { return obj instanceof Boolean b && b; }
}
