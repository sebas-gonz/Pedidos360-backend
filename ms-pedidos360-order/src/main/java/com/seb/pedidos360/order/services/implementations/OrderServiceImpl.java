package com.seb.pedidos360.order.services.implementations;

import com.seb.pedidos360.order.configs.ActivemqConfig;
import com.seb.pedidos360.order.dto.audit.AuditIngestDTO;
import com.seb.pedidos360.order.dto.notify.NotifyInvoiceDTO;
import com.seb.pedidos360.order.dto.notify.NotifyKitchenTicketDTO;
import com.seb.pedidos360.order.dto.notify.NotifyUserDTO;
import com.seb.pedidos360.order.dto.notify.OrderItemKitchenDTO;
import com.seb.pedidos360.order.dto.order.input.OrderCreatedRequestDTO;
import com.seb.pedidos360.order.dto.order.out.OrderItemResponseDTO;
import com.seb.pedidos360.order.dto.order.out.OrderResponseDTO;
import com.seb.pedidos360.order.dto.order.out.ProductItemResponseDTO;
import com.seb.pedidos360.order.dto.product.DeductStockRequestDTO;
import com.seb.pedidos360.order.dto.product.ProductDTO;
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
import jakarta.jms.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import tools.jackson.databind.ObjectMapper;

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
    private final JmsTemplate ttlJmsTemplate;
    private final ObjectMapper objectMapper;
    private final Topic orderEventsTopic;

    @Override
    @Transactional
    public Mono<OrderResponseDTO> createOrder(OrderCreatedRequestDTO orderDTO, String customerId, String userEmail, String userName, String userRole) {
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
                            .statusNumber(0)
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
                                            List<OrderItemResponseDTO> itemDTOs = tuples.stream()
                                                    .map(tuple -> itemOrderToDTO(tuple.getT1(), tuple.getT2()))
                                                    .toList();

                                            List<OrderItemKitchenDTO> itemsTicket = tuples.stream()
                                                    .map(tuple -> {
                                                        OrderItem item = tuple.getT1();
                                                        ProductDTO product = tuple.getT2();
                                                        return new OrderItemKitchenDTO(product.productName(), item.getQuantity()
                                                        );
                                                    }).toList();

                                            NotifyKitchenTicketDTO ticket = new NotifyKitchenTicketDTO(savedOrder.getUuid(), savedOrder.getOrderStatus(), itemsTicket);
                                            NotifyUserDTO notifyUserDTO = new NotifyUserDTO(userEmail, userName, order.getOrderStatus(), order.getUuid());
                                            NotifyInvoiceDTO notifyInvoiceDTO = new NotifyInvoiceDTO(savedOrder.getUuid(), customerId, userName, userEmail, totalOrdenAmount, savedOrder.getPriceCurrency());
                                            AuditIngestDTO auditDTO = new AuditIngestDTO(order.getUuid(),
                                                    "order." + savedOrder.getOrderStatus().toLowerCase(),
                                                    userEmail,
                                                    userRole,
                                                    "Pedido " + savedOrder.getOrderStatus() + " por :" + userName);

                                            try {
                                                String ticketNotification = objectMapper.writeValueAsString(ticket);
                                                String notification = objectMapper.writeValueAsString(notifyUserDTO);
                                                String invoice = objectMapper.writeValueAsString(notifyInvoiceDTO);
                                                String auditMessage = objectMapper.writeValueAsString(auditDTO);

                                                ttlJmsTemplate.convertAndSend(ActivemqConfig.KITCHEN_QUEUE, ticketNotification);
                                                ttlJmsTemplate.convertAndSend(ActivemqConfig.EMAIL_QUEUE, notification);
                                                ttlJmsTemplate.convertAndSend(ActivemqConfig.INVOICE_QUEUE, invoice);
                                                ttlJmsTemplate.convertAndSend(orderEventsTopic, auditMessage);
                                            } catch (Exception e) {
                                                return Mono.error(new RuntimeException(e));
                                            }
                                            List<ReportIngestDTO.ProductQuantityDTO> reportItems = tuples.stream()
                                                    .map(t -> new ReportIngestDTO.ProductQuantityDTO(t.getT1().getProductUuid(), t.getT1().getQuantity())).toList();
                                            ReportIngestDTO reportDTO = new ReportIngestDTO(savedOrder.getUuid(), savedOrder.getOrderStatus(), savedOrder.getTotalOrderAmount(), Instant.now(), reportItems);

                                            return Mono.when(
                                                    notifyClient.sentOrderCreatedNotification(notifyUserDTO).onErrorResume(e -> Mono.empty()),
                                                    auditClient.ingestAudit(auditDTO).onErrorResume(e -> Mono.empty()),
                                                    reportClient.ingestReport(reportDTO).onErrorResume(e -> Mono.empty())
                                            ).thenReturn(orderToDTO(savedOrder, itemDTOs));
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
                        .flatMap(orderItem ->
                                catalogClient.getProductByUuid(orderItem.getProductUuid())
                                        .map(productDTO -> itemOrderToDTO(orderItem, productDTO)))
                        .collectList()
                        .map(orderItemResponseDTOS -> orderToDTO(orderFound, orderItemResponseDTOS))
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderResponseDTO> findOrdersByUserId(String userId) {
        return orderRepository.findByCustomerId(userId)
                .flatMap(orderFound -> orderItemRepository.findByOrderId(orderFound.getId())
                        .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                .map(productDTO -> itemOrderToDTO(orderItem, productDTO))
                        )
                        .collectList()
                        .map(orderItems -> orderToDTO(orderFound, orderItems))
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(orderFound -> orderItemRepository.findByOrderId(orderFound.getId())
                        .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                .map(productDTO -> itemOrderToDTO(orderItem, productDTO))
                        )
                        .collectList()
                        .map(orderItems -> orderToDTO(orderFound, orderItems))
                );
    }

    @Override
    public Flux<OrderResponseDTO> getOrdersByStatus(String status) {
        return orderRepository.findByOrderStatus(status)
                .flatMap(orderFound -> orderItemRepository.findByOrderId(orderFound.getId())
                        .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                .map(productDTO -> itemOrderToDTO(orderItem, productDTO))
                        )
                        .collectList()
                        .map(orderItems -> orderToDTO(orderFound, orderItems))
                );
    }

    @Override
    @Transactional()
    public Mono<OrderResponseDTO> updateOrderStatus(UUID orderUuid,
                                                    String customerId,
                                                    String userEmail,
                                                    String userName,
                                                    String userRole) {
        return orderRepository.findByUuid(orderUuid)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order with UUID " + orderUuid + " not found")))
                .flatMap(orderFound -> {
                    if (orderFound.getStatusNumber().compareTo(-1) < 0) {
                        return Mono.error(new IllegalArgumentException("Order with UUID " + orderUuid + " is already canceled"));
                    }
                    Order updatedOrder = updateOrderStatus(orderFound);

                    Mono<Void> stockDeductionMono = Mono.empty();
                    if (updatedOrder.getOrderStatus().equals(OrderStatus.ACCEPTED.toString())) {
                        stockDeductionMono = orderItemRepository.findByOrderId(orderFound.getId())
                                .map(itemOrder -> new DeductStockRequestDTO(itemOrder.getProductUuid(), itemOrder.getQuantity()))
                                .collectList()
                                .flatMap(catalogClient::deductStock);
                    }
                    return stockDeductionMono.then(r2Template.update(updatedOrder));
                })
                .flatMap(orderUpdated -> {

                    NotifyUserDTO notifyUserDTO = new NotifyUserDTO(
                            userEmail,
                            userName,
                            orderUpdated.getOrderStatus(),
                            orderUpdated.getUuid()
                    );
                    AuditIngestDTO auditDTO = new AuditIngestDTO(orderUpdated.getUuid(),
                            "order." + orderUpdated.getOrderStatus().toLowerCase(),
                            userEmail,
                            userRole,
                            "Estado del pedido actualizado a " + orderUpdated.getOrderStatus() + " por :" + userName);
                    try {
                        String notification = objectMapper.writeValueAsString(notifyUserDTO);
                        String auditMessage = objectMapper.writeValueAsString(auditDTO);

                        ttlJmsTemplate.convertAndSend(orderEventsTopic, auditMessage);
                        ttlJmsTemplate.convertAndSend(ActivemqConfig.EMAIL_QUEUE, notification);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException(e));
                    }
                    ReportIngestDTO reportDTO = new ReportIngestDTO(orderUpdated.getUuid(),
                            orderUpdated.getOrderStatus(),
                            orderUpdated.getTotalOrderAmount(),
                            Instant.now(), List.of());

                    return Mono.when(
                                    reportClient.ingestReport(reportDTO).onErrorResume(e -> Mono.empty())
                            )
                            .then(orderItemRepository.findByOrderId(orderUpdated.getId())
                                    .flatMap(orderItem -> catalogClient.getProductByUuid(orderItem.getProductUuid())
                                            .map(productDTO -> itemOrderToDTO(orderItem, productDTO)))
                                    .collectList()
                            )
                            .map(orderItems -> orderToDTO(orderUpdated, orderItems));
                });
    }

    @Override
    public Mono<OrderResponseDTO> cancelOrder(UUID orderUuid, String userId, String userEmail, String userName,
                                              String userRole) {
        return orderRepository.findByUuid(orderUuid)
                .flatMap(orderFound -> {
                    Order canceledOrder = cancelOrder(orderFound);
                    return orderRepository.save(canceledOrder)
                            .flatMap(orderCanceled -> {
                                        NotifyUserDTO notifyUserDTO = new NotifyUserDTO(
                                                userEmail,
                                                userName,
                                                canceledOrder.getOrderStatus(),
                                                canceledOrder.getUuid()
                                        );
                                        AuditIngestDTO audit = new AuditIngestDTO(
                                                orderFound.getUuid(),
                                                "order.canceled",
                                                userEmail,
                                                userRole,
                                                "Pedido cancelado por: " + userName
                                        );
                                        try {
                                            String notification = objectMapper.writeValueAsString(notifyUserDTO);
                                            String auditMessage = objectMapper.writeValueAsString(audit);

                                            ttlJmsTemplate.convertAndSend(orderEventsTopic, auditMessage);
                                            ttlJmsTemplate.convertAndSend(ActivemqConfig.EMAIL_QUEUE, notification);
                                        } catch (Exception e) {
                                            return Mono.error(new RuntimeException(e));
                                        }
                                        return orderItemRepository.findByOrderId(orderCanceled.getId())
                                                .flatMap(orderItem -> catalogClient
                                                        .getProductByUuid(orderItem.getProductUuid())
                                                        .map(productDTO -> itemOrderToDTO(orderItem, productDTO))
                                                )
                                                .collectList()
                                                .map(orderItems -> orderToDTO(orderCanceled, orderItems));
                                    }
                            );
                });
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
                                        .map(productDTO -> itemOrderToDTO(orderItem, productDTO));
                            })
                            .collectList()
                            .map(orderItems -> orderToDTO(orders, orderItems));
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
                                        .map(productDTO -> itemOrderToDTO(orderItem, productDTO));
                            })
                            .collectList()
                            .map(orderItems -> orderToDTO(orders, orderItems));
                });
    }

    private OrderResponseDTO orderToDTO(Order order, List<OrderItemResponseDTO> orderItems) {
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

    private OrderItemResponseDTO itemOrderToDTO(OrderItem orderItem, ProductDTO productDTO) {
        return new OrderItemResponseDTO(
                orderItem.getUuid(),
                orderItem.getQuantity(),
                orderItem.getTotalItemAmount(),
                productDTOtoProductItemResponseDTO(productDTO)
        );
    }

    private ProductItemResponseDTO productDTOtoProductItemResponseDTO(ProductDTO productDTO) {
        return new ProductItemResponseDTO(
                productDTO.productUuid(),
                productDTO.version(),
                productDTO.price(),
                productDTO.productName(),
                productDTO.images()
        );
    }

    private Order updateOrderStatus(Order order) {
        Integer numberOfStatus = order.getStatusNumber();
        switch (numberOfStatus) {
            case 0:
                order.setOrderStatus(OrderStatus.ACCEPTED.toString());
                order.setStatusNumber(numberOfStatus + 1);
                return order;
            case 1:
                order.setOrderStatus(OrderStatus.PREPARING.toString());
                order.setStatusNumber(numberOfStatus + 1);
                return order;
            case 2:
                order.setOrderStatus(OrderStatus.DISPATCHED.toString());
                order.setStatusNumber(numberOfStatus + 1);
                return order;
            case 3:
                order.setOrderStatus(OrderStatus.DELIVERED.toString());
                order.setStatusNumber(numberOfStatus + 1);
                return order;
            default:
                return order;
        }
    }

    private Order cancelOrder(Order order) {
        order.setOrderStatus(OrderStatus.CANCELED.toString());
        order.setStatusNumber(-1);
        return order;
    }
}

