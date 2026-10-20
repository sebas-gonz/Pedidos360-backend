package com.seb.pedidos360.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QueueRequestDTO(@NotBlank(message = "El nombre de la cola no puede estar vacío")
                              @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola contiene caracteres inválidos. Solo usa letras, números, puntos y guiones.")
                              String queueName) {
}
