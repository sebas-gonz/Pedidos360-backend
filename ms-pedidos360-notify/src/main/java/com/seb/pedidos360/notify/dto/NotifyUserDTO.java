package com.seb.pedidos360.notify.dto;

import java.util.UUID;

public record NotifyUserDTO(String customerEmail,
                            String customerName,
                            String orderStatus,
                            UUID orderUuid) {
}