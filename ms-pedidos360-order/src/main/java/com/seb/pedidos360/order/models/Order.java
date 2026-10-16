package com.seb.pedidos360.order.models;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table(schema = "orders", name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Order extends BaseEntity{

    private @Id Long id;

    @Column("customer_id")
    private String customerId;

    @Column("total_order_amount")
    private BigDecimal totalOrderAmount;

    @Column("price_currency")
    private String priceCurrency;

    @Column("order_status")
    private String orderStatus;

    @Column("status_n")
    @Builder.Default
    private Integer statusNumber = 0;

}
