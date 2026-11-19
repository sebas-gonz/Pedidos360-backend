package com.seb.pedidos360.report.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record ProductQuantityDTO(@NotNull(message = "El productUuid no puede ser nulo")
                                 UUID productUuid,

                                 @NotNull(message = "La cantidad es obligatoria")
                                 @PositiveOrZero(message = "La cantidad no puede ser negativa")
                                 Integer quantity) {}
