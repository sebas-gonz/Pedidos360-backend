package com.seb.pedidos360.audit.repositories;

import com.seb.pedidos360.audit.entities.AuditEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends ReactiveCrudRepository<AuditEvent, Long> {
    @Query("SELECT * FROM audit.audit_events WHERE created_at >= :fromDate AND created_at <= :toDate ORDER BY created_at DESC")
    Flux<AuditEvent> findByCreatedAtBetween(Instant fromDate, Instant toDate);

    Flux<AuditEvent> findByOrderUuidOrderByCreatedAtDesc(UUID orderUuid);
}
