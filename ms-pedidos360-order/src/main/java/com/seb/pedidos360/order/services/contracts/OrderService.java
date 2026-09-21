package com.seb.pedidos360.order.services.contracts;

import com.seb.pedidos360.order.dto.order.input.OrderCreatedRequestDTO;
import com.seb.pedidos360.order.dto.order.input.UpdateOrderStatus;
import com.seb.pedidos360.order.dto.order.out.OrderResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface OrderService {
    Mono<OrderResponseDTO> createOrder(OrderCreatedRequestDTO orderDTO, String customerId, String customerEmail, String customerName);
    Mono<OrderResponseDTO> findOrderByUuid(UUID orderUuid);

    Flux<OrderResponseDTO> findOrdersByUserId(String userId);
    Flux<OrderResponseDTO> getAllOrders();
    Flux<OrderResponseDTO> getOrdersByStatus(String status);
    Mono<OrderResponseDTO> updateOrderStatus(UUID orderUuid, UpdateOrderStatus updateOrderStatusDTO, String customerId, String customerEmail, String customerName);
    Flux<OrderResponseDTO> getOrdersByStatusAndBetweenDates(String status, Instant fromDate, Instant toDate);

    Flux<OrderResponseDTO> getOrdersByStatusBetweenDatesAndUserid(String status, String userid, Instant fromDate, Instant toDate);
}
