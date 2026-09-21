package com.seb.pedidos360.order.dto.product;

import java.util.UUID;

public record DeductStockRequestDTO(UUID productUuid, Integer quantity) {
}
