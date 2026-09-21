package com.seb.pedidos360.report.repositories;

import com.seb.pedidos360.report.entities.ReportOrder;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public interface ReportOrderRepository extends ReactiveCrudRepository<ReportOrder, Long> {
    Mono<ReportOrder> findByOrderUuid(UUID orderUuid);

    @Query("SELECT COUNT(id) FROM reports.report_orders WHERE created_at >= :since")
    Mono<Long> countOrdersSince(Instant since);

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM reports.report_orders WHERE created_at >= :since")
    Mono<BigDecimal> sumSalesSince(Instant since);

    @Query("SELECT COUNT(id) FROM reports.report_orders WHERE status NOT IN ('ENTREGADO', 'CANCELADO')")
    Mono<Long> countActiveOrders();

    @Query("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (delivered_at - created_at)) / 60), 0) " +
            "FROM reports.report_orders WHERE status = 'ENTREGADO' AND created_at >= :since")
    Mono<Double> averageLeadTimeSince(Instant since);
}
