package com.seb.pedidos360.notify.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record NotifyInvoiceDTO(UUID orderUuid,
                               String customerId,
                               String customerName,
                               String customerEmail,
                               BigDecimal totalOrderAmount,
                               String priceCurrency) {
}
