package com.seb.pedidos360.catalog.repositories;

import com.seb.pedidos360.catalog.models.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    Mono<Product> findByUuid(UUID uuid);

    Flux<Product> findAll();
}
