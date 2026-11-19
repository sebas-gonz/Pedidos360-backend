package com.seb.pedidos360.catalog.dto.product.input;

import java.util.UUID;

public record DeductStockRequestDTO(UUID productUuid, Integer quantity) {
}
