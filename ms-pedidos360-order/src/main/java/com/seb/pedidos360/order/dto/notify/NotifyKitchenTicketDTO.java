package com.seb.pedidos360.order.dto.notify;

import com.seb.pedidos360.order.dto.order.out.OrderItemResponseDTO;

import java.util.List;
import java.util.UUID;

public record NotifyKitchenTicketDTO(UUID orderUuid,
                                     String orderStatus,
                                     List<OrderItemKitchenDTO> items) {
}
