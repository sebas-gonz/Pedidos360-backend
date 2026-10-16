package com.seb.pedidos360.order.dto.order.out;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductItemResponseDTO(UUID productUuid,
                                     Integer version,
                                     BigDecimal price,
                                     String productName,
                                     Set<String> imagesUrl) {


}
