package com.seb.pedidos360.administrator.dto.kafka.response;

import java.util.List;

public record PartitionInfoDTO(
        int partition,
        int leaderId,
        List<Integer> replicas,
        List<Integer> isr
) {
}
