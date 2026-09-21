package com.seb.pedidos360.order.services.implementations;

import com.seb.pedidos360.order.models.OrderItem;
import com.seb.pedidos360.order.repositories.OrderItemRepository;
import com.seb.pedidos360.order.repositories.OrderRepository;
import com.seb.pedidos360.order.services.contracts.OrderItemService;
import com.seb.pedidos360.order.services.contracts.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.UUID;
@Component
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository  orderRepository;

    @Override
    public Flux<OrderItem> findAllItemsByOrderId(UUID orderId) {
        return orderRepository.findByUuid(orderId)
                .flatMapMany(orderFound -> orderItemRepository.findByOrderId(orderFound.getId()))
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order with id: " + orderId + " not found")));
    }
}
