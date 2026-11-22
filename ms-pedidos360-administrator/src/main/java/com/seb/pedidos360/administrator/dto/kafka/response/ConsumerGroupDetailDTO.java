package com.seb.pedidos360.administrator.dto.kafka.response;

import java.util.List;

public record ConsumerGroupDetailDTO(
        String groupId,
        String state,
        int coordinatorId,
        String partitionAssignor,
        long totalLag,
        List<ConsumerMemberDTO> members,
        List<ConsumerGroupOffsetDTO> offsets
) {
}
