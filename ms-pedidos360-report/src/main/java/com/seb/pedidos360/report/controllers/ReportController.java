package com.seb.pedidos360.report.controllers;

import com.seb.pedidos360.report.dto.KpiResponseDTO;
import com.seb.pedidos360.report.dto.ReportIngestDTO;
import com.seb.pedidos360.report.dto.TopProductDTO;
import com.seb.pedidos360.report.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/kpis")
    public Mono<KpiResponseDTO> getKpis(@RequestParam(defaultValue = "last24h") String range) {
        return reportService.getKpis(range);
    }

    @GetMapping("/top-products")
    public Flux<TopProductDTO> getTopProducts(@RequestParam(defaultValue = "last7d") String range) {
        return reportService.getTopProducts(range);
    }

    @PostMapping("/ingest")
    public Mono<Void> ingestEvent(@RequestBody ReportIngestDTO ingestDTO) {
        return reportService.ingestOrderEvent(ingestDTO);
    }
}
