package com.seb.pedidos360.order.dto.notify;

import java.math.BigDecimal;
import java.util.UUID;

public record NotifyDTO(String userEmail, String userName, BigDecimal totalAmount, String currency,
                        String orderStatus,
                        UUID orderId
                        ) {
}