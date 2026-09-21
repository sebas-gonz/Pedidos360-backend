package com.seb.pedidos360.order.dto.order.input;

import java.util.List;

public record OrderCreatedRequestDTO(String priceCurrency, List<OrderListRequestDTO> orderItems) {
}

