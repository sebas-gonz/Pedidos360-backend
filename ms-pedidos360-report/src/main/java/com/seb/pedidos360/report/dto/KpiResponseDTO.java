package com.seb.pedidos360.report.dto;

import java.math.BigDecimal;

public record KpiResponseDTO(
        Long totalOrders,
        BigDecimal totalSales,
        Long activeOrders,
        Double averageLeadTimeMinutes
) {
}
