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
    @Query("SELECT * FROM audit_events WHERE event_date >= :fromDate AND event_date <= :toDate ORDER BY event_date DESC")
    Flux<AuditEvent> findByEventDateBetween(Instant fromDate, Instant toDate);

    Flux<AuditEvent> findByOrderUuidOrderByEventDateDesc(UUID orderUuid);
}
