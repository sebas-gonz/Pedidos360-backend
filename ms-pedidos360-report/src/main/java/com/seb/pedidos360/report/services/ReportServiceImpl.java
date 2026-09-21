package com.seb.pedidos360.report.services;

import com.seb.pedidos360.report.dto.KpiResponseDTO;
import com.seb.pedidos360.report.dto.ReportIngestDTO;
import com.seb.pedidos360.report.dto.TopProductDTO;
import com.seb.pedidos360.report.entities.ReportOrder;
import com.seb.pedidos360.report.entities.ReportOrderItem;
import com.seb.pedidos360.report.repositories.ReportOrderItemRepository;
import com.seb.pedidos360.report.repositories.ReportOrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportOrderRepository orderRepository;
    private final ReportOrderItemRepository itemRepository;

    @Override
    public Mono<KpiResponseDTO> getKpis(String range) {
        Instant since = calculateSinceDate(range);

        return Mono.zip(
                orderRepository.countOrdersSince(since),
                orderRepository.sumSalesSince(since),
                orderRepository.countActiveOrders(),
                orderRepository.averageLeadTimeSince(since)
        ).map(tuple -> new KpiResponseDTO(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4()
        ));
    }

    @Override
    public Flux<TopProductDTO> getTopProducts(String range) {
        Instant since = calculateSinceDate(range);
        return itemRepository.findTopProductsSince(since);
    }

    @Override
    @Transactional
    public Mono<Void> ingestOrderEvent(ReportIngestDTO ingestDTO) {
        return orderRepository.findByOrderUuid(ingestDTO.orderUuid())
                .flatMap(existingOrder -> {
                    existingOrder.setStatus(ingestDTO.status());
                    if ("ENTREGADO".equals(ingestDTO.status())) {
                        existingOrder.setDeliveredAt(ingestDTO.timestamp());
                    }
                    return orderRepository.save(existingOrder).then();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ReportOrder newOrder = ReportOrder.builder()
                            .orderUuid(ingestDTO.orderUuid())
                            .status(ingestDTO.status())
                            .totalAmount(ingestDTO.totalAmount())
                            .createdAt(ingestDTO.timestamp())
                            .build();

                    return orderRepository.save(newOrder)
                            .flatMap(savedOrder -> {
                                List<ReportOrderItem> items = ingestDTO.items().stream()
                                        .map(item -> ReportOrderItem.builder()
                                                .reportOrderId(savedOrder.getId())
                                                .productUuid(item.productUuid())
                                                .quantity(item.quantity())
                                                .build())
                                        .toList();
                                return itemRepository.saveAll(items).then();
                            });
                }));
    }

    private Instant calculateSinceDate(String range) {
        return switch (range) {
            case "last7d" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "last30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> Instant.now().minus(24, ChronoUnit.HOURS);
        };
    }
}
