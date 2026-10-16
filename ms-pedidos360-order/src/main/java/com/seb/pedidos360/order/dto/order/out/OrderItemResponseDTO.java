package com.seb.pedidos360.order.dto.order.out;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponseDTO(UUID orderItemUuid,
                                   Integer quantity,
                                   BigDecimal totalOrderItemAmount,
                                   ProductItemResponseDTO product){

}
