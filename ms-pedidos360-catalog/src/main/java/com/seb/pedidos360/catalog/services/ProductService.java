package com.seb.pedidos360.catalog.services;

import com.seb.pedidos360.catalog.dto.product.input.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.product.input.DeductStockRequestDTO;
import com.seb.pedidos360.catalog.dto.product.input.UpdateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.product.output.ProductResponseDTO;
import com.seb.pedidos360.catalog.models.Product;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    Mono<ProductResponseDTO> saveProduct(CreateProductRequestDTO productDTO, Flux<FilePart> imageFiles, String userEmail, String userName, String userRole);
    Mono<ProductResponseDTO> updateProduct(UpdateProductRequestDTO updatedProduct, UUID productId, String userEmail, String userName, String userRole);
    Mono<ProductResponseDTO> getProductByUuid(UUID productUuid);
    Flux<ProductResponseDTO> getAllProducts();

    Flux<ProductResponseDTO> deductStock(List<DeductStockRequestDTO>  deductStockRequests, String userEmail, String userName, String userRole);
}
