package com.seb.pedidos360.order.dto.order.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record OrderResponseDTO(UUID orderUuid,
                               String customerId,
                               BigDecimal totalOrderAmount,
                               String orderStatus,
                               String priceCurrency,
                               Integer version,
                               List<OrderItemResponseDTO> orderItems
) {
    public record OrderItemResponseDTO(UUID orderItemUuid,
                                       Integer quantity,
                                       BigDecimal totalOrderItemAmount,
                                       ProductItemResponseDTO product) {
        public record ProductItemResponseDTO(UUID productUuid,
                                             Integer version,
                                             BigDecimal price,
                                             String productName,
                                             Set<String> imagesUrl) {
        }

    }

}

