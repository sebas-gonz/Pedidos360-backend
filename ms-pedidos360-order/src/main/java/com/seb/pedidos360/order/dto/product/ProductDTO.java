package com.seb.pedidos360.order.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductDTO(
        @JsonProperty("uuid")
        UUID productUuid,
        String productName, String description,
        BigDecimal price,
        Long stock,
        String priceCurrency,
        Set<String> tags,
        @JsonProperty("imagesUrl")
        Set<String> images,
        Integer version) {
}
