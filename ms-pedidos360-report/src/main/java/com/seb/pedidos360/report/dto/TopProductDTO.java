package com.seb.pedidos360.report.dto;

import java.util.UUID;

public record TopProductDTO(
        UUID productUuid,
        Long totalSold
) {
}
