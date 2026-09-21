package com.seb.pedidos360.report.entities;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;
@Getter @Setter
@Table(name = "report_order_items", schema = "reports")
@Builder
public class ReportOrderItem {
    @Id
    private Long id;
    private Long reportOrderId;
    private UUID productUuid;
    private Integer quantity;
}
