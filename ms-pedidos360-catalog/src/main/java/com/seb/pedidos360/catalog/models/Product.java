package com.seb.pedidos360.catalog.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.javamoney.moneta.Money;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Table(schema = "catalog", name = "products")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class Product extends BaseEntity {

    private @Id Long id;

    @Column("product_name")
    private String productName;

    private String description;

    private BigDecimal price;

    private Long stock;

    @Column("price_currency")
    private String priceCurrency;

    private Set<String> tags = new HashSet<>();

    private Set<String> imagesUrl = new HashSet<>();

    @Transient
    @JsonIgnore
    public MonetaryAmount getMonetaryPrice() {
        return Money.of(this.price, Monetary.getCurrency(this.priceCurrency));
    }
}
