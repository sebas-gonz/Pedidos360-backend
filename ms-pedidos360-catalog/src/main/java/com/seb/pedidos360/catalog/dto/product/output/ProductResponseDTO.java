package com.seb.pedidos360.catalog.dto.product.output;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductResponseDTO(UUID productUuid,
                                 String productName,
                                 String description,
                                 BigDecimal price,
                                 Long stock,
                                 String priceCurrency,
                                 Set<String> tags,
                                 Set<String> imagesUrl,
                                 Integer version) {
}
