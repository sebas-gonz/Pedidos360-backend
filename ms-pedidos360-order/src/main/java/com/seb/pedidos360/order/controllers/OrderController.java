package com.seb.pedidos360.order.controllers;

import com.seb.pedidos360.order.dto.order.input.OrderCreatedRequestDTO;
import com.seb.pedidos360.order.dto.order.input.UpdateOrderStatus;
import com.seb.pedidos360.order.dto.order.out.OrderResponseDTO;
import com.seb.pedidos360.order.models.OrderItem;
import com.seb.pedidos360.order.models.OrderStatus;
import com.seb.pedidos360.order.services.contracts.OrderItemService;
import com.seb.pedidos360.order.services.contracts.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @PostMapping
    public Mono<ResponseEntity<OrderResponseDTO>> createOrder(@Valid @RequestBody Mono<OrderCreatedRequestDTO> orderRequest,
                                                              @RequestHeader("X-Customer-Id") String customerId,
                                                              @RequestHeader("X-Customer-Email") String customerEmail,
                                                              @RequestHeader("X-Customer-Name") String customerName,
                                                              ServerWebExchange exchange) {
        return orderRequest.flatMap(order -> orderService.createOrder(order, customerId, customerEmail, customerName))
                .map(savedOrder -> {
                    URI location = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                            .path("/{orderId}")
                            .buildAndExpand(savedOrder.orderUuid())
                            .toUri();
                    return ResponseEntity.created(location)
                            .eTag(savedOrder.version().toString())
                            .body(savedOrder);
                });
    }
    @GetMapping
    public Flux<OrderResponseDTO> getOrders(){
        return orderService.getAllOrders();
    }

    @GetMapping("/{uuid}")
    public Mono<ResponseEntity<OrderResponseDTO>> getOrderByUuid(@PathVariable("uuid") UUID uuid,
                                                                 ServerWebExchange exchange) {
        return orderService.findOrderByUuid(uuid)
                .map(orderFound -> {
                    String etag = orderFound.version().toString();
                    if (exchange.checkNotModified(etag)) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                    return ResponseEntity.ok()
                            .eTag(etag)
                            .body(orderFound);
                });
    }

    @PutMapping("/{uuid}/status")
    public Mono<ResponseEntity<OrderResponseDTO>> updateOrderStatus(@PathVariable("uuid") UUID uuid,
                                                                    @RequestBody Mono<UpdateOrderStatus> newOrderStatus,
                                                                    @RequestHeader("X-Customer-Id") String customerId,
                                                                    @RequestHeader("X-Customer-Email") String customerEmail,
                                                                    @RequestHeader("X-Customer-Name") String customerName) {
        return newOrderStatus.flatMap(status -> orderService.updateOrderStatus(uuid, status, customerId, customerEmail, customerName))
                .map(updatedOrder -> ResponseEntity.ok()
                        .eTag(updatedOrder.version().toString())
                        .body(updatedOrder));
    }

    @GetMapping("/table")
    public Flux<OrderResponseDTO> findOrderByStatusBetweenDates(@RequestParam(name = "status", defaultValue = "CREATED") String status,
                                                                @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                                                @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
                                                                ServerWebExchange exchange) {
        return orderService.getOrdersByStatusAndBetweenDates(status, fromDate, toDate);
    }

    @GetMapping("/{userid}/table")
    public Flux<OrderResponseDTO> findOrdersByStatusBetweenDatesAndUserid(@PathVariable("userid") String userId,
                                                                          @RequestParam(name = "status", defaultValue = "CREATED") String status,
                                                                          @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                                                          @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
                                                                          ServerWebExchange exchange) {
        return orderService.getOrdersByStatusBetweenDatesAndUserid(userId, status, fromDate, toDate);
    }

    @GetMapping("/{uuid}/items")
    public Flux<OrderItem> findItemsByOrderUuid(@PathVariable("uuid") UUID uuid, ServerWebExchange exchange) {
        return orderItemService.findAllItemsByOrderId(uuid);
    }

    @GetMapping("/users/{userid}")
    public Flux<OrderResponseDTO> findOrdersByUserId(@PathVariable("userid") String userId, ServerWebExchange exchange) {
        return orderService.findOrdersByUserId(userId);
    }

    @GetMapping("/status/{status}")
    public Flux<OrderResponseDTO> findOrdersByStatus(@PathVariable String status,ServerWebExchange exchange) {
        return orderService.getOrdersByStatus(status);
    }
}
