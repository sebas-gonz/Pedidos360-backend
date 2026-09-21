package com.seb.pedidos360.report.repositories;

import com.seb.pedidos360.report.dto.TopProductDTO;
import com.seb.pedidos360.report.entities.ReportOrderItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface ReportOrderItemRepository extends ReactiveCrudRepository<ReportOrderItem, Long> {
    @Query("SELECT i.product_uuid, SUM(i.quantity) as total_sold " +
            "FROM report_order_items i " +
            "JOIN report_orders o ON i.report_order_id = o.id " +
            "WHERE o.created_at >= :since " +
            "GROUP BY i.product_uuid " +
            "ORDER BY total_sold DESC " +
            "FETCH FIRST 5 ROWS ONLY")
    Flux<TopProductDTO> findTopProductsSince(Instant since);
}
