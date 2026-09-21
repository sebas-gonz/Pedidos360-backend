package com.seb.pedidos360.order.repositories;

import com.seb.pedidos360.order.dto.order.out.OrderResponseDTO;
import com.seb.pedidos360.order.models.Order;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Mono<Order> findByUuid(UUID uuid);
    @Query("SELECT o.* FROM orders.ORDERS o WHERE o.created_at BETWEEN :from AND :to AND o.status = :status")
    Flux<Order> filterOrderByDates(@Param("from") Instant from,
                                   @Param("to") Instant to,
                                   @Param("status") String status);

    @Query("SELECT o.* FROM orders.ORDERS o WHERE o.created_at BETWEEN :from AND :to AND o.status = :status " +
            "AND o.customerId = :customerid")
    Flux<Order> filterOrdersByStatusBetweenDatesAndUserid(@Param("from") Instant from,
                                                          @Param("to") Instant to,
                                                          @Param("status") String status,
                                                          @Param("customerid") String customerId);

    Flux<Order> findByCustomerId(String customerId);

    Flux<Order> findByOrderStatus(String orderStatus);
}
