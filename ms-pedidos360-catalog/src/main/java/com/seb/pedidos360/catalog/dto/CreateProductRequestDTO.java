package com.seb.pedidos360.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record CreateProductRequestDTO(
        @NotBlank(message = "El nombre del producto no puede estar vacio")
        @Size(min = 3, max = 100, message = "El nombre del producto debe tener entre 3 y 100 caracteres")
        String productName,

        @NotBlank(message = "La descripcion del producto no puede estar vacia")
        @Size(min = 10, message = "La descripcion del producto debe tener al menos 10 caracteres")
        String description,

        @NotNull
        @Min(value = 1, message = "El precio del producto no puede ser menor a $1 CLP")
        BigDecimal price,

        @NotNull
        @Min(value = 0, message = "El stock del producto no puede ser negativo")
        Long stock,

        @NotBlank(message = "El tipo de moneda no puede estar vacío")
        String priceCurrency,

        @Size(min = 1, max = 10, message = "El producto debe tener entre 1 y 10 etiquetas")
        List<String> tags,
        @Size(max = 5, message = "No puedes subir más de 5 imágenes")
        Set<String> imagesUrl
) {
}
