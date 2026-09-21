package com.seb.pedidos360.report.services;

import com.seb.pedidos360.report.dto.KpiResponseDTO;
import com.seb.pedidos360.report.dto.ReportIngestDTO;
import com.seb.pedidos360.report.dto.TopProductDTO;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReportService {
    Mono<KpiResponseDTO> getKpis(String range);

    Flux<TopProductDTO> getTopProducts(String range);

    Mono<Void> ingestOrderEvent(ReportIngestDTO ingestDTO);
}
