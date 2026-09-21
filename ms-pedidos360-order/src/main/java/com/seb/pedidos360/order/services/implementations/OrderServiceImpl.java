package com.seb.pedidos360.order.services.implementations;

import com.seb.pedidos360.order.dto.audit.AuditIngestDTO;
import com.seb.pedidos360.order.dto.order.input.OrderCreatedRequestDTO;
import com.seb.pedidos360.order.dto.product.DeductStockRequestDTO;
import com.seb.pedidos360.order.dto.product.ProductDTO;
import com.seb.pedidos360.order.dto.order.input.UpdateOrderStatus;
import com.seb.pedidos360.order.dto.notify.NotifyDTO;
import com.seb.pedidos360.order.dto.order.out.OrderResponseDTO;
import com.seb.pedidos360.order.dto.report.ReportIngestDTO;
import com.seb.pedidos360.order.models.Order;
import com.seb.pedidos360.order.models.OrderItem;
import com.seb.pedidos360.order.models.OrderStatus;
import com.seb.pedidos360.order.repositories.OrderItemRepository;
import com.seb.pedidos360.order.repositories.OrderRepository;
import com.seb.pedidos360.order.rest.AuditClient;
import com.seb.pedidos360.order.rest.CatalogClient;
import com.seb.pedidos360.order.rest.NotifyClient;
import com.seb.pedidos360.order.rest.ReportClient;
import com.seb.pedidos360.order.services.contracts.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final CatalogClient catalogClient;
    private final OrderRepository orderRepository;
    private final R2dbcEntityTemplate r2Template;
    private final OrderItemRepository orderItemRepository;
    private final NotifyClient notifyClient;
    private final ReportClient reportClient;
    private final AuditClient auditClient;

    @Override
    @Transactional
    public Mono<OrderResponseDTO> createOrder(OrderCreatedRequestDTO orderDTO, String customerId, String customerEmail, String customerName) {
        return Flux.fromIterable(orderDTO.orderItems())
                .flatMap(orderListRequestDTO -> catalogClient.getProductByUuid(orderListRequestDTO.productUuid())
                        .map(productDTO -> {
                            BigDecimal totalItemProduct = productDTO.price()
                                    .multiply(BigDecimal.valueOf(orderListRequestDTO.quantity()));

                            OrderItem orderItem = OrderItem.builder()
                                    .priceCurrency(productDTO.priceCurrency())
                                    .quantity(orderListRequestDTO.quantity())
                                    .productUuid(productDTO.productUuid())
                                    .totalItemAmount(totalItemProduct)
                                    .build();

                            return Tuples.of(orderItem, productDTO);
                        }))
                .collectList()
                .flatMap(tuples -> {
                    BigDecimal totalOrdenAmount = tuples.stream()
                            .map(tuple -> tuple.getT1().getTotalItemAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Order order = Order
                            .builder()
                            .orderStatus(OrderStatus.CREATED.toString())
                            .totalOrderAmount(totalOrdenAmount)
                            .priceCurrency(orderDTO.priceCurrency())
                            .customerId(customerId)
                            .build();

                    return r2Template.insert(order)
                            .flatMap(savedOrder -> {

                                List<OrderItem> itemsToSave = tuples.stream()
                                        .map(tuple -> {
                                            OrderItem item = tuple.getT1();
                                            item.setOrderId(savedOrder.getId());
                                            return item;
                                        })
                                        .toList();

                                return orderItemRepository.saveAll(itemsToSave)
                                        .collectList()
                                        .flatMap(savedItems -> {
                                            List<OrderResponseDTO.OrderItemResponseDTO> itemDTOs = tuples.stream()
                                                    .map(tuple -> mapToOrderItemResponseDTO(tuple.getT1(), tuple.getT2()))
                                                    .toList();
                                            NotifyDTO notifyDTO = new NotifyDTO(customerEmail, customerName, order.getTotalOrderAmount(), order.getPriceCurrency(), order.getOrderStatus(), savedOrder.getUuid());

                                            AuditIngestDTO auditDTO = new AuditIngestDTO(savedOrder.getUuid(), "OrderCreated", customerEmail, "Customer", "Pedido creado con éxito");

                                            List<ReportIngestDTO.ProductQuantityDTO> reportItems = tuples.stream()
                                                    .map(t -> new ReportIngestDTO.ProductQuantityDTO(t.getT1().getProductUuid(), t.getT1().getQuantity())).toList();
                                            ReportIngestDTO reportDTO = new ReportIngestDTO(savedOrder.getUuid(), savedOrder.getOrderStatus(), savedOrder.getTotalOrderAmount(), Instant.now(), reportItems);

                                            return Mono.when(
                                                    notifyClient.sentOrderCreatedNotification(notifyDTO).onErrorResume(e -> Mono.empty()),
                                                    auditClient.ingestAudit(auditDTO).onErrorResume(e -> Mono.empty()),
                                                    reportClient.ingestReport(reportDTO).onErrorResume(e -> Mono.empty())
                                            ).thenReturn(mapToOrderResponseDTO(savedOrder, itemDTOs));
                                        });

                            });
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OrderResponseDTO> findOrderByUuid(UUID orderUuid) {
        return orderRepository.findByUuid(orderUuid)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order with UUID " + orderUuid + " not found")))
                .flatMap(orderFound -> orderItemRepository.findByOrderId(orderFound.getId())
                        .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                .map(productDTO -> mapToOrderItemResponseDTO(orderItem, productDTO))
                        )
                        .collectList()
                        .map(orderItems -> mapToOrderResponseDTO(orderFound, orderItems))
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderResponseDTO> findOrdersByUserId(String userId) {
        return orderRepository.findByCustomerId(userId)
                .flatMap(orderFound -> orderItemRepository.findByOrderId(orderFound.getId())
                        .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                .map(productDTO -> mapToOrderItemResponseDTO(orderItem, productDTO))
                        )
                        .collectList()
                        .map(orderItems -> mapToOrderResponseDTO(orderFound, orderItems))
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(orderFound -> orderItemRepository.findByOrderId(orderFound.getId())
                        .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                .map(productDTO -> mapToOrderItemResponseDTO(orderItem, productDTO))
                        )
                        .collectList()
                        .map(orderItems -> mapToOrderResponseDTO(orderFound, orderItems))
                );
    }

    @Override
    public Flux<OrderResponseDTO> getOrdersByStatus(String status) {
        return orderRepository.findByOrderStatus(status)
                .flatMap(orderFound -> orderItemRepository.findByOrderId(orderFound.getId())
                        .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                .map(productDTO -> mapToOrderItemResponseDTO(orderItem, productDTO))
                        )
                        .collectList()
                        .map(orderItems -> mapToOrderResponseDTO(orderFound, orderItems))
                );
    }

    @Override
    @Transactional()
    public Mono<OrderResponseDTO> updateOrderStatus(UUID orderUuid,
                                                    UpdateOrderStatus updateOrderStatusDTO,
                                                    String customerId,
                                                    String customerEmail,
                                                    String customerName) {
        return orderRepository.findByUuid(orderUuid)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order with UUID " + orderUuid + " not found")))
                .flatMap(orderFound -> {
                    String newStatus = updateOrderStatusDTO.orderStatus();
                    String oldStatus = orderFound.getOrderStatus();
                    if (newStatus.equals(OrderStatus.DISPATCHED.toString()) && oldStatus.equals(OrderStatus.CREATED.toString())) {
                        return Mono.error(new IllegalArgumentException("Regla de negocio: Un pedido no puede pasar a DESPACHADO si previamente no fue ACEPTADO."));
                    }

                    orderFound.setOrderStatus(newStatus);

                    Mono<Void> stockDeductionMono = Mono.empty();
                    if (newStatus.equals(OrderStatus.ACCEPTED.toString()) && !oldStatus.equals(OrderStatus.ACCEPTED.toString())) {
                        stockDeductionMono = orderItemRepository.findByOrderId(orderFound.getId())
                                .map(orderItem -> {
                                    return new DeductStockRequestDTO(orderItem.getProductUuid(), orderItem.getQuantity());
                                })
                                .collectList()
                                .flatMap(catalogClient::deductStock);
                    }
                    return stockDeductionMono.then(r2Template.update(orderFound));
                })
                .flatMap(orderUpdated -> {

                    NotifyDTO notifyDTO = new NotifyDTO(customerEmail,
                            customerName,
                            orderUpdated.getTotalOrderAmount(),
                            orderUpdated.getPriceCurrency(),
                            orderUpdated.getOrderStatus(),
                            orderUpdated.getUuid());

                    AuditIngestDTO auditDTO = new AuditIngestDTO(orderUpdated.getUuid(),
                            "Order" + orderUpdated.getOrderStatus(),
                            customerEmail,
                            "Operator",
                            "Estado del pedido actualizado a " + orderUpdated.getOrderStatus());

                    ReportIngestDTO reportDTO = new ReportIngestDTO(orderUpdated.getUuid(),
                            orderUpdated.getOrderStatus(),
                            orderUpdated.getTotalOrderAmount(),
                            Instant.now(), List.of());

                    return Mono.when(
                                    notifyClient.setUpdatedOrderNotification(notifyDTO).onErrorResume(e -> Mono.empty()),
                                    auditClient.ingestAudit(auditDTO).onErrorResume(e -> Mono.empty()),
                                    reportClient.ingestReport(reportDTO).onErrorResume(e -> Mono.empty())
                            )
                            .then(orderItemRepository.findByOrderId(orderUpdated.getId())
                                    .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                            .map(productDTO -> mapToOrderItemResponseDTO(orderItem, productDTO)))
                                    .collectList()
                            )
                            .map(orderItems -> mapToOrderResponseDTO(orderUpdated, orderItems)); });
    }

    @Override
    @Transactional
    public Flux<OrderResponseDTO> getOrdersByStatusAndBetweenDates(String status,
                                                                   Instant fromDate,
                                                                   Instant toDate) {
        return orderRepository.filterOrderByDates(fromDate, toDate, status)
                .flatMap(orders -> {
                    return orderItemRepository.findByOrderId(orders.getId())
                            .flatMap(orderItem -> {
                                return catalogClient.getProductByUuid(orderItem.getProductUuid())
                                        .map(productDTO -> mapToOrderItemResponseDTO(orderItem, productDTO));
                            })
                            .collectList()
                            .map(orderItems -> mapToOrderResponseDTO(orders, orderItems));
                });
    }

    @Override
    @Transactional
    public Flux<OrderResponseDTO> getOrdersByStatusBetweenDatesAndUserid(String status,
                                                                         String userid,
                                                                         Instant fromDate,
                                                                         Instant toDate) {
        return orderRepository.filterOrdersByStatusBetweenDatesAndUserid(fromDate, toDate, status, userid)
                .flatMap(orders -> {
                    return orderItemRepository.findByOrderId(orders.getId())
                            .flatMap(orderItem -> {
                                return catalogClient.getProductByUuid(orderItem.getProductUuid())
                                        .map(productDTO -> mapToOrderItemResponseDTO(orderItem, productDTO));
                            })
                            .collectList()
                            .map(orderItems -> mapToOrderResponseDTO(orders, orderItems));
                });
    }

    private OrderResponseDTO mapToOrderResponseDTO(Order order,
                                                   List<OrderResponseDTO.OrderItemResponseDTO> orderItems) {
        return new OrderResponseDTO(
                order.getUuid(),
                order.getCustomerId(),
                order.getTotalOrderAmount(),
                order.getOrderStatus(),
                order.getPriceCurrency(),
                order.getVersion(),
                orderItems
        );
    }

    private OrderResponseDTO.OrderItemResponseDTO mapToOrderItemResponseDTO(OrderItem orderItem,
                                                                            ProductDTO productDTO) {
        return new OrderResponseDTO.OrderItemResponseDTO(
                orderItem.getUuid(),
                orderItem.getQuantity(),
                orderItem.getTotalItemAmount(),
                mapToProductItemResponseDTO(productDTO)
        );
    }

    private OrderResponseDTO.OrderItemResponseDTO.ProductItemResponseDTO mapToProductItemResponseDTO(ProductDTO productDTO) {
        return new OrderResponseDTO.OrderItemResponseDTO.ProductItemResponseDTO(
                productDTO.productUuid(),
                productDTO.version(),
                productDTO.price(),
                productDTO.productName(),
                productDTO.images()
        );
    }
}

