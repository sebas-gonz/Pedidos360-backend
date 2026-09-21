package com.seb.pedidos360.order.dto.order.input;

import java.util.UUID;

public record OrderListRequestDTO(UUID productUuid, Integer quantity){
}
