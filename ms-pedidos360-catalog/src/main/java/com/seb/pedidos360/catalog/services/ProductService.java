package com.seb.pedidos360.catalog.services;

import com.seb.pedidos360.catalog.dto.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.DeductStockRequestDTO;
import com.seb.pedidos360.catalog.dto.UpdateProductRequestDTO;
import com.seb.pedidos360.catalog.models.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    Mono<Product> saveProduct(CreateProductRequestDTO productDTO);
    Mono<Product> updateProduct(UpdateProductRequestDTO updatedProduct, UUID productId);
    Mono<Product> getProductByUuid(UUID productUuid);
    Flux<Product> getAllProducts();

    Flux<Product> deductStock(List<DeductStockRequestDTO>  deductStockRequests);
}
