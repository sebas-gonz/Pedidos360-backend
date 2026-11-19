package com.seb.pedidos360.report.listeners;

import com.seb.pedidos360.report.dto.ReportIngestDTO;
import com.seb.pedidos360.report.services.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumeOrderEvent {
    private final ReportService reportService;
    @KafkaListener(topics = "${kafka.topics.orders}", groupId = "report-service-group")
    public void consumeOrderEvent(@Valid ReportIngestDTO ingestDTO) {
        reportService.ingestOrderEvent(ingestDTO)
                .doOnSuccess(v -> log.debug("Métricas guardadas para la orden: {}", ingestDTO.orderUuid()))
                .doOnError(e -> log.error("Error al procesar datos de la orden {}: {}", ingestDTO.orderUuid(), e.getMessage()))
                .block(Duration.ofSeconds(10));
    }
}
