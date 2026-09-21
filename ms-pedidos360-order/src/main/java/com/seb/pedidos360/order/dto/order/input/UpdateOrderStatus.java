package com.seb.pedidos360.order.dto.order.input;

import com.seb.pedidos360.order.models.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatus(
        @NotNull(message = "Debe existir un nuevo estado de la orden.")
        String orderStatus) {
}
