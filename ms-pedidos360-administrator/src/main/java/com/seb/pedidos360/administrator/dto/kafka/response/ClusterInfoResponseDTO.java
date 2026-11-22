package com.seb.pedidos360.administrator.dto.kafka.response;

import java.util.List;

public record ClusterInfoResponseDTO(String clusterId,
                                     int controllerId,
                                     List<BrokerInfoDTO> brokers) {
}
