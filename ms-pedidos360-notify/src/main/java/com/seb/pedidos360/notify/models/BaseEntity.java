package com.seb.pedidos360.notify.models;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {

    private Instant createdAt;

    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID uuid = UUID.randomUUID();
}
