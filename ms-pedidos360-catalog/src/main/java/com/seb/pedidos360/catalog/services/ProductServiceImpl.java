package com.seb.pedidos360.catalog.services;

import com.seb.pedidos360.catalog.dto.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.DeductStockRequestDTO;
import com.seb.pedidos360.catalog.dto.UpdateProductRequestDTO;
import com.seb.pedidos360.catalog.mappers.ProductMapper;
import com.seb.pedidos360.catalog.models.Product;
import com.seb.pedidos360.catalog.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final R2dbcEntityTemplate r2Template;

    @Override
    @Transactional
    public Mono<Product> saveProduct(CreateProductRequestDTO productDTO) {
        Product product = productMapper.toEntity(productDTO);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Mono<Product> updateProduct(UpdateProductRequestDTO updatedProduct, UUID productUuid) {
        return productRepository.findByUuid(productUuid)
                .flatMap(product -> {
                    product.setPrice(updatedProduct.price());
                    product.setStock(updatedProduct.stock());
                    return r2Template.update(product);
                })
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product with id: " + productUuid + " does not exist")));
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Product> getProductByUuid(UUID productUuid) {
        return productRepository.findByUuid(productUuid)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product with id: " + productUuid + " does not exist")));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Flux<Product> deductStock(List<DeductStockRequestDTO> deductStockRequests) {
        return Flux.fromIterable(deductStockRequests)
                .flatMap(deductStockRequestDTO -> productRepository.findByUuid(deductStockRequestDTO.productUuid())
                        .switchIfEmpty(Mono.error(new NoSuchElementException("Product with uuid: " + deductStockRequestDTO.productUuid() + " not found")))
                        .flatMap(product -> {
                            if (product.getStock() > deductStockRequestDTO.quantity()) {
                                Long stockRestante = product.getStock() - deductStockRequestDTO.quantity();
                                product.setStock(stockRestante);
                                return r2Template.update(product);
                            }
                            return Mono.error(new IllegalArgumentException("Insuficient stock"));
                        })
                );

    }
}
