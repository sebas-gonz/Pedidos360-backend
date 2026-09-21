package com.seb.pedidos360.notify.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notify extends BaseEntity {
    private String message;
    private String email;
    private String userId;
    private BigDecimal totalAmount;
    private String currency;

}
