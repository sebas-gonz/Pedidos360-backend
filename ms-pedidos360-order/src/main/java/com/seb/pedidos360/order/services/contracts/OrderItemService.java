package com.seb.pedidos360.order.services.contracts;

import com.seb.pedidos360.order.models.OrderItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OrderItemService{
    Flux<OrderItem> findAllItemsByOrderId(UUID orderId);
}
