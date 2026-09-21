package com.seb.pedidos360.order.rest;

import com.seb.pedidos360.order.dto.product.DeductStockRequestDTO;
import com.seb.pedidos360.order.dto.product.ProductDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
public class CatalogClient {
    private final WebClient webClient;

    public CatalogClient(WebClient.Builder webClient,
                         @Value("${app.clients.catalog.url}")
                         String catalogUrl) {
        this.webClient = webClient.baseUrl(catalogUrl).build();
    }

    public Mono<ProductDTO> getProductByUuid(UUID productUuid){
        return webClient.get()
                .uri("/api/catalog/products/{uuid}", productUuid)
                .retrieve()
                .bodyToMono(ProductDTO.class);
    }

    public Mono<Void> deductStock(List<DeductStockRequestDTO>  itemsToDeduct){
        return webClient.post()
                .uri("/api/catalog/products/deduct-stock")
                .bodyValue(itemsToDeduct)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
