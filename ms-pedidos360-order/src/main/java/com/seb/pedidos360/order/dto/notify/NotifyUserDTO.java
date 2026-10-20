package com.seb.pedidos360.order.dto.notify;

import java.math.BigDecimal;
import java.util.UUID;

public record NotifyUserDTO(String customerEmail,
                            String customerName,
                            String orderStatus,
                            UUID orderUuid) {
}