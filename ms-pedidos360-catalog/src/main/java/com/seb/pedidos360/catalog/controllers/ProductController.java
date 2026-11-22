package com.seb.pedidos360.catalog.controllers;

import com.seb.pedidos360.catalog.dto.product.input.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.product.input.DeductStockRequestDTO;
import com.seb.pedidos360.catalog.dto.product.input.UpdateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.product.output.ProductResponseDTO;
import com.seb.pedidos360.catalog.models.Product;
import com.seb.pedidos360.catalog.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
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
    private final ProductService productService;

    @GetMapping("/products")
    public Flux<ProductResponseDTO> getAllProducts(ServerWebExchange exchange) {
        return productService.getAllProducts();
    }

    @GetMapping("/products/{uuid}")
    public Mono<ResponseEntity<ProductResponseDTO>> getProductByUuid(@PathVariable("uuid") UUID uuid, ServerWebExchange exchange) {
        return productService.getProductByUuid(uuid)
                .map(product -> {
                    String etag = product.version().toString();
                    if (exchange.checkNotModified(etag)) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                    return ResponseEntity.ok()
                            .eTag(etag)
                            .body(product);
                });
    }

    @PostMapping(value = "/create-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ProductResponseDTO>> createProduct(@RequestPart("product") CreateProductRequestDTO productDTO,
                                                       @RequestPart("images") Flux<FilePart> imageFiles,
                                                       @RequestHeader("X-Customer-Email") String customerEmail,
                                                       @RequestHeader("X-Customer-Name") String customerName,
                                                       @RequestHeader("X-User-Roles") String userRole,
                                                       ServerWebExchange exchange) {
        return productService.saveProduct(productDTO, imageFiles, customerEmail, customerName, userRole)
                .map(savedProduct -> {
                    URI location = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                            .path("/{uuid}")
                            .buildAndExpand(savedProduct.productUuid())
                            .toUri();
                    return ResponseEntity.created(location)
                            .eTag(savedProduct.version().toString())
                            .body(savedProduct);
                });
    }

    @PutMapping("/products/{uuid}")
    public Mono<ResponseEntity<ProductResponseDTO>> updateProduct(@PathVariable("uuid") UUID uuid, @RequestBody UpdateProductRequestDTO updatedProductDTO,
                                                       @RequestHeader("X-Customer-Email") String customerEmail,
                                                       @RequestHeader("X-Customer-Name") String customerName,
                                                       @RequestHeader("X-User-Roles") String userRole,
                                                       ServerWebExchange exchange) {
        return productService.updateProduct(updatedProductDTO, uuid, customerEmail, customerName, userRole)
                .map(updatedProduct -> {
                    String etag = updatedProduct.version().toString();
                    if (exchange.checkNotModified(etag)) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                    return ResponseEntity.ok()
                            .eTag(etag)
                            .body(updatedProduct);
                });
    }

    @PostMapping("/products/deduct-stock")
    public Flux<ProductResponseDTO> deductStock(@RequestBody List<DeductStockRequestDTO> itemsToDeduct,
                                     @RequestHeader("X-Customer-Email") String customerEmail,
                                     @RequestHeader("X-Customer-Name") String customerName,
                                     @RequestHeader("X-User-Roles") String userRole) {
        return productService.deductStock(itemsToDeduct,  customerEmail, customerName, userRole);
    }
}
