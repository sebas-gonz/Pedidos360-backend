package com.seb.pedidos360.administrator.dto.kafka.response;

import java.util.Map;

public record ClusterHealthDTO(
        String clusterId,
        int controllerId,
        int activeBrokers,
        int totalTopics,
        int totalPartitions,
        int underReplicatedPartitions,
        int offlinePartitions,
        Map<Integer, Integer> leaderCountPerBroker,
        Map<String, Long> messageCountPerTopic,
        Map<String, Long> dltAlerts
) {
}
