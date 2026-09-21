package com.seb.pedidos360.catalog.controllers;

import com.seb.pedidos360.catalog.dto.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.DeductStockRequestDTO;
import com.seb.pedidos360.catalog.dto.UpdateProductRequestDTO;
import com.seb.pedidos360.catalog.models.Product;
import com.seb.pedidos360.catalog.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService  productService;

    @GetMapping("/products")
    public Flux<Product> getAllProducts(ServerWebExchange exchange) {
        return productService.getAllProducts();
    }

    @GetMapping("/products/{uuid}")
    public Mono<ResponseEntity<Product>> getProductByUuid(@PathVariable("uuid") UUID uuid, ServerWebExchange exchange) {
        return productService.getProductByUuid(uuid)
                .map(product -> {
                    String etag = product.getVersion().toString();
                    if (exchange.checkNotModified(etag)) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                    return ResponseEntity.ok()
                            .eTag(etag)
                            .body(product);
                });
    }

    @PostMapping("/products")
    public Mono<ResponseEntity<Product>> createProduct(@RequestBody CreateProductRequestDTO productDTO, ServerWebExchange exchange) {
        return productService.saveProduct(productDTO)
                .map(savedProduct -> {
                    URI location = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                            .path("/{uuid}")
                            .buildAndExpand(savedProduct.getUuid())
                            .toUri();
                    return ResponseEntity.created(location)
                            .eTag(savedProduct.getVersion().toString())
                            .body(savedProduct);
                });
    }

    @PutMapping("/products/{uuid}")
    public Mono<ResponseEntity<Product>> updateProduct(@PathVariable("uuid") UUID uuid,@RequestBody UpdateProductRequestDTO updatedProductDTO, ServerWebExchange exchange){
        return productService.updateProduct(updatedProductDTO, uuid)
                .map(updatedProduct -> {
                    String etag = updatedProduct.getVersion().toString();
                    if (exchange.checkNotModified(etag)) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                    return ResponseEntity.ok()
                            .eTag(etag)
                            .body(updatedProduct);
                });
    }

    @PostMapping("/products/deduct-stock")
    public Flux<Product> deductStock(@RequestBody List<DeductStockRequestDTO> itemsToDeduct) {
        return productService.deductStock(itemsToDeduct);
    }
}
