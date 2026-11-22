package com.seb.pedidos360.administrator.dto.kafka.response;

import java.util.List;
import java.util.Map;

public record TopicDetailResponseDTO(String name,
                                     boolean internal,
                                     int partitionCount,
                                     int replicationFactor,
                                     List<PartitionInfoDTO> partitions,
                                     Map<String, String> configs) {
}
