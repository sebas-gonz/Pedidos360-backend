package com.seb.pedidos360.catalog.dto.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportDTO(UUID orderUuid, String status, BigDecimal totalAmount, Instant timestamp, List<ProductQuantityReportDTO> items) {
}
