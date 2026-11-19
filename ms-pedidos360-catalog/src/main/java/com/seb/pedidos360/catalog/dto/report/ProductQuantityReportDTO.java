package com.seb.pedidos360.catalog.dto.report;

import java.util.UUID;

public record ProductQuantityReportDTO(UUID productUuid, Integer quantity) {
}
