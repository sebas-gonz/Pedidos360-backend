package com.seb.pedidos360.administrator.dto.kafka.response;

import java.util.List;

public record ConsumerMemberDTO(String consumerId,
                                String clientId,
                                String host,
                                List<String> assignedPartitions) {
}
