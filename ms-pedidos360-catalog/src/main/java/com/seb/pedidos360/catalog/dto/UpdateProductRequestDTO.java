package com.seb.pedidos360.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateProductRequestDTO(@NotNull
                                      @Min(value = 1, message = "El precio del producto no puede ser menor a $1 CLP")
                                      BigDecimal price,

                                      @NotNull
                                      @Min(value = 0, message = "El stock del producto no puede ser negativo")
                                      Long stock) {
}
