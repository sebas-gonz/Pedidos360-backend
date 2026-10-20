package com.seb.pedidos360.notify.dto;

import java.util.List;
import java.util.UUID;

public record NotifyKitchenTicketDTO(UUID orderUuid,
                                     String orderStatus,
                                     List<OrderItemKitchenDTO> items) {
}
