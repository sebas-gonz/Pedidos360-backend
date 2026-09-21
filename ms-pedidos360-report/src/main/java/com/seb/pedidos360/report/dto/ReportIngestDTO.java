package com.seb.pedidos360.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportIngestDTO(
        UUID orderUuid,
        String status,
        BigDecimal totalAmount,
        Instant timestamp,
        List<ProductQuantityDTO> items
) {
    public record ProductQuantityDTO(UUID productUuid, Integer quantity) {}
}
