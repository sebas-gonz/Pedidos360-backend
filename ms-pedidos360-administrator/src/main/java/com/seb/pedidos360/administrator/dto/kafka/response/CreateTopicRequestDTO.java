package com.seb.pedidos360.administrator.dto.kafka.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record CreateTopicRequestDTO(@NotBlank(message = "El nombre del tópico es obligatorio")
                                    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, números, puntos, guiones y guion bajo")
                                    String name,

                                    @Min(value = 1, message = "Debe tener al menos 1 partición")
                                    int partitions,

                                    @Min(value = 1, message = "El factor de replicación mínimo es 1")
                                    @Max(value = 3, message = "El clúster cuenta con máximo 3 brokers")
                                    short replicationFactor,
                                    Map<String, String> configs
) {}
