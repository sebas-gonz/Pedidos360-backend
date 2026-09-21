package com.seb.pedidos360.order.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table(schema = "orders", name = "order_items")
@SuperBuilder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends BaseEntity{
    private @Id Long id;

    @Column("product_uuid")
    private UUID productUuid;

    @Column("order_id")
    private Long orderId;

    private Integer quantity;

    @Column("total_item_amount")
    private BigDecimal totalItemAmount;

    @Column("price_currency")
    private String priceCurrency;

}
