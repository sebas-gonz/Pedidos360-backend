package com.seb.pedidos360.report.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Table(name = "report_orders", schema = "reports")
@Getter @Setter
@Builder
public class ReportOrder {
    @Id
    private Long id;
    private UUID orderUuid;
    private String status;
    private BigDecimal totalAmount;
    private Instant createdAt;
    private Instant deliveredAt;
}
